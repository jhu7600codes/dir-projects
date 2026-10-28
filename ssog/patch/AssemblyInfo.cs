namespace System.Runtime.CompilerServices
{
	// unity's mscorlib only has the enum constructor, csc wants the int one
	[AttributeUsage(AttributeTargets.Assembly | AttributeTargets.Module)]
	internal sealed class CompilationRelaxationsAttribute : Attribute
	{
		public CompilationRelaxationsAttribute(int relaxations)
		{
		}
	}
}
