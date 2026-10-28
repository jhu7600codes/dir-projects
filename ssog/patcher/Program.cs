// copies SsogPatch.dll's classes into the game's Assembly-CSharp.dll and hooks them into DailyWord.
// no new dll in the apk, unity 3.5 only knows the assemblies it shipped with.
//   dotnet run -- <Managed dir> <SsogPatch.dll> <output Assembly-CSharp.dll>
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Mono.Cecil;
using Mono.Cecil.Cil;

class Program
{
	static ModuleDefinition target;
	static ModuleDefinition patch;
	static readonly Dictionary<TypeDefinition, TypeDefinition> types = new Dictionary<TypeDefinition, TypeDefinition>();
	static readonly Dictionary<MethodDefinition, MethodDefinition> methods = new Dictionary<MethodDefinition, MethodDefinition>();
	static readonly Dictionary<FieldDefinition, FieldDefinition> fields = new Dictionary<FieldDefinition, FieldDefinition>();

	static int Main(string[] args)
	{
		if (args.Length != 3)
		{
			Console.Error.WriteLine("usage: patcher <Managed dir> <SsogPatch.dll> <out Assembly-CSharp.dll>");
			return 2;
		}
		var resolver = new DefaultAssemblyResolver();
		resolver.AddSearchDirectory(args[0]);
		var rp = new ReaderParameters { AssemblyResolver = resolver, ReadingMode = ReadingMode.Immediate };
		target = ModuleDefinition.ReadModule(Path.Combine(args[0], "Assembly-CSharp.dll"), rp);
		patch = ModuleDefinition.ReadModule(args[1], rp);
		if (target.GetType("SsogCheats") != null)
		{
			Console.Error.WriteLine("Assembly-CSharp.dll is already patched");
			return 1;
		}

		var copy = patch.Types.Where(t => t.Name != "<Module>" && t.FullName != "System.Runtime.CompilerServices.CompilationRelaxationsAttribute").ToList();
		foreach (var t in copy)
		{
			DeclareType(t, null);
		}
		foreach (var pair in types)
		{
			CopyMembers(pair.Key, pair.Value);
		}
		foreach (var pair in methods)
		{
			CopyBody(pair.Key, pair.Value);
		}

		Hook();
		Check();
		target.Write(args[2]);
		Console.WriteLine("patched " + string.Join(", ", copy.Select(t => t.Name)));
		return 0;
	}

	static void DeclareType(TypeDefinition src, TypeDefinition outer)
	{
		string name = src.Name;
		// the game has its own <PrivateImplementationDetails> already
		if (outer == null && name.StartsWith("<PrivateImplementationDetails>"))
		{
			name += "Ssog";
		}
		var dst = new TypeDefinition(src.Namespace, name, src.Attributes)
		{
			ClassSize = src.ClassSize,
			PackingSize = src.PackingSize
		};
		types[src] = dst;
		if (outer == null)
		{
			target.Types.Add(dst);
		}
		else
		{
			outer.NestedTypes.Add(dst);
		}
		foreach (var nested in src.NestedTypes)
		{
			DeclareType(nested, dst);
		}
	}

	static void CopyMembers(TypeDefinition src, TypeDefinition dst)
	{
		dst.BaseType = src.BaseType == null ? null : Map(src.BaseType);
		foreach (var f in src.Fields)
		{
			var nf = new FieldDefinition(f.Name, f.Attributes, Map(f.FieldType))
			{
				InitialValue = f.InitialValue
			};
			if (f.HasConstant)
			{
				nf.Constant = f.Constant;
			}
			dst.Fields.Add(nf);
			fields[f] = nf;
		}
		foreach (var m in src.Methods)
		{
			var nm = new MethodDefinition(m.Name, m.Attributes, target.TypeSystem.Void)
			{
				ImplAttributes = m.ImplAttributes,
				HasThis = m.HasThis,
				ExplicitThis = m.ExplicitThis,
				CallingConvention = m.CallingConvention
			};
			dst.Methods.Add(nm);
			nm.ReturnType = Map(m.ReturnType);
			foreach (var p in m.Parameters)
			{
				nm.Parameters.Add(new ParameterDefinition(p.Name, p.Attributes, Map(p.ParameterType)));
			}
			methods[m] = nm;
		}
	}

	static void CopyBody(MethodDefinition src, MethodDefinition dst)
	{
		if (!src.HasBody)
		{
			return;
		}
		var body = dst.Body;
		body.InitLocals = src.Body.InitLocals;
		body.MaxStackSize = src.Body.MaxStackSize;
		foreach (var v in src.Body.Variables)
		{
			body.Variables.Add(new VariableDefinition(Map(v.VariableType)));
		}
		var map = new Dictionary<Instruction, Instruction>();
		foreach (var i in src.Body.Instructions)
		{
			var ni = Instruction.Create(OpCodes.Nop);
			ni.OpCode = i.OpCode;
			ni.Operand = i.Operand switch
			{
				TypeReference t => Map(t),
				MethodReference m => Map(m),
				FieldReference f => Map(f),
				VariableDefinition v => body.Variables[v.Index],
				ParameterDefinition p => p.Index < 0 ? body.ThisParameter : dst.Parameters[p.Index],
				_ => i.Operand
			};
			body.Instructions.Add(ni);
			map[i] = ni;
		}
		foreach (var ni in body.Instructions)
		{
			if (ni.Operand is Instruction t)
			{
				ni.Operand = map[t];
			}
			else if (ni.Operand is Instruction[] ts)
			{
				ni.Operand = ts.Select(x => map[x]).ToArray();
			}
		}
		foreach (var h in src.Body.ExceptionHandlers)
		{
			body.ExceptionHandlers.Add(new ExceptionHandler(h.HandlerType)
			{
				TryStart = Get(map, h.TryStart),
				TryEnd = Get(map, h.TryEnd),
				HandlerStart = Get(map, h.HandlerStart),
				HandlerEnd = Get(map, h.HandlerEnd),
				FilterStart = Get(map, h.FilterStart),
				CatchType = h.CatchType == null ? null : Map(h.CatchType)
			});
		}
	}

	static Instruction Get(Dictionary<Instruction, Instruction> map, Instruction i) => i == null ? null : map[i];


	static bool FromGame(TypeReference t) => t.Scope is AssemblyNameReference a && a.Name == target.Assembly.Name.Name;

	static TypeReference Map(TypeReference t)
	{
		switch (t)
		{
		case GenericInstanceType g:
			var ng = new GenericInstanceType(Map(g.ElementType));
			foreach (var a in g.GenericArguments)
			{
				ng.GenericArguments.Add(Map(a));
			}
			return ng;
		case ArrayType a:
			return new ArrayType(Map(a.ElementType), a.Rank);
		case ByReferenceType b:
			return new ByReferenceType(Map(b.ElementType));
		case GenericParameter:
			return t;
		case TypeDefinition d when types.ContainsKey(d):
			return types[d];
		}
		if (FromGame(t))
		{
			return target.GetType(t.FullName) ?? throw new Exception("not in the game: " + t.FullName);
		}
		if (t.Scope == patch)
		{
			return types[t.Resolve()];
		}
		return target.ImportReference(t);
	}

	static MethodReference Map(MethodReference m)
	{
		if (m is GenericInstanceMethod gm)
		{
			var ngm = new GenericInstanceMethod(Map(gm.ElementMethod));
			foreach (var a in gm.GenericArguments)
			{
				ngm.GenericArguments.Add(Map(a));
			}
			return ngm;
		}
		if (m is MethodDefinition md && methods.ContainsKey(md))
		{
			return methods[md];
		}
		var declaring = Map(m.DeclaringType);
		if (declaring is TypeDefinition td)
		{
			// a game method, use the real definition
			var found = td.Methods.FirstOrDefault(x => x.Name == m.Name && x.Parameters.Count == m.Parameters.Count
				&& x.Parameters.Select(p => p.ParameterType.FullName).SequenceEqual(m.Parameters.Select(p => p.ParameterType.FullName)));
			return found ?? throw new Exception("method not in the game: " + m.FullName);
		}
		var nm = new MethodReference(m.Name, target.TypeSystem.Void, declaring)
		{
			HasThis = m.HasThis,
			ExplicitThis = m.ExplicitThis,
			CallingConvention = m.CallingConvention
		};
		foreach (var gp in m.GenericParameters)
		{
			nm.GenericParameters.Add(new GenericParameter(gp.Name, nm));
		}
		nm.ReturnType = Map(m.ReturnType);
		foreach (var p in m.Parameters)
		{
			nm.Parameters.Add(new ParameterDefinition(Map(p.ParameterType)));
		}
		return nm;
	}

	static FieldReference Map(FieldReference f)
	{
		if (f is FieldDefinition fd && fields.ContainsKey(fd))
		{
			return fields[fd];
		}
		var declaring = Map(f.DeclaringType);
		if (declaring is TypeDefinition td)
		{
			return td.Fields.First(x => x.Name == f.Name);
		}
		return new FieldReference(f.Name, Map(f.FieldType), declaring);
	}

	// DailyWord: no more server download, apply cheats on start and when the game comes back
	static void Hook()
	{
		var dailyWord = target.GetType("DailyWord");
		var sync = types.Values.First(t => t.Name == "SsogDaily").Methods.First(m => m.Name == "Sync");
		var apply = types.Values.First(t => t.Name == "SsogCheats").Methods.First(m => m.Name == "Apply");

		var forceSync = dailyWord.Methods.First(m => m.Name == "ForceSync");
		Rewrite(forceSync, il =>
		{
			il.Emit(OpCodes.Call, sync);
			il.Emit(OpCodes.Ret);
		});

		var start = dailyWord.Methods.First(m => m.Name == "Start");
		Rewrite(start, il =>
		{
			il.Emit(OpCodes.Call, apply);
			il.Emit(OpCodes.Ldarg_0);
			il.Emit(OpCodes.Call, forceSync);
			il.Emit(OpCodes.Ret);
		});

		var pause = new MethodDefinition("OnApplicationPause", MethodAttributes.Private | MethodAttributes.HideBySig, target.TypeSystem.Void);
		pause.Parameters.Add(new ParameterDefinition("paused", ParameterAttributes.None, target.TypeSystem.Boolean));
		dailyWord.Methods.Add(pause);
		var il2 = pause.Body.GetILProcessor();
		var ret = il2.Create(OpCodes.Ret);
		il2.Emit(OpCodes.Ldarg_1);
		il2.Emit(OpCodes.Brtrue, ret);
		il2.Emit(OpCodes.Call, apply);
		il2.Emit(OpCodes.Call, sync);
		il2.Append(ret);
	}

	static void Rewrite(MethodDefinition m, Action<ILProcessor> emit)
	{
		m.Body.Instructions.Clear();
		m.Body.ExceptionHandlers.Clear();
		m.Body.Variables.Clear();
		emit(m.Body.GetILProcessor());
	}

	// every type, method and field the new code touches has to exist in unity's stripped dlls
	static void Check()
	{
		var bad = new List<string>();
		foreach (var m in types.Values.SelectMany(t => t.Methods).Where(m => m.HasBody))
		{
			foreach (var i in m.Body.Instructions)
			{
				try
				{
					var ok = i.Operand switch
					{
						MethodReference r => r.Resolve() != null,
						FieldReference r => r.Resolve() != null,
						TypeReference r => r.Resolve() != null,
						_ => true
					};
					if (!ok)
					{
						bad.Add(m.FullName + ": " + i.Operand);
					}
				}
				catch (Exception ex)
				{
					bad.Add(m.FullName + ": " + i.Operand + " (" + ex.Message + ")");
				}
			}
		}
		if (bad.Count > 0)
		{
			throw new Exception("unresolved:\n" + string.Join("\n", bad));
		}
	}
}
