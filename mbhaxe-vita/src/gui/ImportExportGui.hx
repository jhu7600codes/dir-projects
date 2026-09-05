package gui;

import haxe.DynamicAccess;
import src.MarbleGame;
import hxd.res.BitmapFont;
import h3d.Vector;
import src.ResourceLoader;
import src.Settings;
import src.Util;

class ImportExportGui extends GuiImage {
	var innerCtrl:GuiControl;

	public function new() {
		var res = ResourceLoader.getImage("data/ui/xbox/BG_fadeOutSoftEdge.png").resource.toTile();
		super(res);
		var domcasual32fontdata = ResourceLoader.getFileEntry("data/font/DomCasualD.fnt");
		var domcasual32b = new BitmapFont(domcasual32fontdata.entry);
		@:privateAccess domcasual32b.loader = ResourceLoader.loader;
		var domcasual32 = domcasual32b.toSdfFont(cast 42 * Settings.uiScale, MultiChannel);

		this.horizSizing = Width;
		this.vertSizing = Height;
		this.position = new Vector();
		this.extent = new Vector(640, 480);

		#if hl
		var scene2d = hxd.Window.getInstance();
		#end
		#if (js || uwp)
		var scene2d = MarbleGame.instance.scene2d;
		#end

		var offsetX = (scene2d.width - 1280) / 2;
		var offsetY = (scene2d.height - 720) / 2;

		var subX = 640 - (scene2d.width - offsetX) * 640 / scene2d.width;
		var subY = 480 - (scene2d.height - offsetY) * 480 / scene2d.height;

		innerCtrl = new GuiControl();
		innerCtrl.position = new Vector(offsetX, offsetY);
		innerCtrl.extent = new Vector(640 - subX, 480 - subY);
		innerCtrl.horizSizing = Width;
		innerCtrl.vertSizing = Height;
		this.addChild(innerCtrl);

		var coliseumfontdata = ResourceLoader.getFileEntry("data/font/ColiseumRR.fnt");
		var coliseumb = new BitmapFont(coliseumfontdata.entry);
		@:privateAccess coliseumb.loader = ResourceLoader.loader;
		var coliseum = coliseumb.toSdfFont(cast 44 * Settings.uiScale, MultiChannel);

		var rootTitle = new GuiText(coliseum);
		rootTitle.position = new Vector(100, 30);
		rootTitle.extent = new Vector(1120, 80);
		rootTitle.text.textColor = 0xFFFFFF;
		rootTitle.text.text = "IMPORT & EXPORT";
		rootTitle.text.alpha = 0.5;
		innerCtrl.addChild(rootTitle);

		var btnList = new GuiXboxList();
		btnList.position = new Vector(70 - offsetX, 165);
		btnList.horizSizing = Left;
		btnList.extent = new Vector(502, 500);
		innerCtrl.addChild(btnList);

		btnList.addButton(0, 'Import Progress', (e) -> {
			hxd.File.browse((sel) -> {
				sel.load((data) -> {
					try {
						// convert to string
						var jsonStr = data.toString();
						// parse JSON
						var json = haxe.Json.parse(jsonStr);

						var highScoreData:DynamicAccess<Array<Score>> = json.highScores;
						for (key => value in highScoreData) {
							Settings.highScores.set(key, value);
						}
						var easterEggData:DynamicAccess<Float> = json.easterEggs;
						if (easterEggData != null) {
							for (key => value in easterEggData) {
								Settings.easterEggs.set(key, value);
							}
						}
						MarbleGame.canvas.pushDialog(new MessageBoxOkDlg("Progress data imported successfully!"));
						Settings.save();
					} catch (e) {
						MarbleGame.canvas.pushDialog(new MessageBoxOkDlg("Failed to import progress data: " + e.message));
					}
				});
			}, {
				title: "Select a progress file to import",
				fileTypes: [
					{name: "JSON files", extensions: ["json"]},
					{name: "All files", extensions: ["*"]}
				],
			});
		});
		btnList.addButton(0, 'Export Progress', (e) -> {
			#if sys
			#if MACOS_BUNDLE
			// open the finder to that folder
			Sys.command('open "${Settings.settingsDir}"');
			#else
			// Just open the folder in the explorer.exe
			Sys.command('explorer.exe "${Settings.settingsDir}"');
			#end
			MarbleGame.canvas.pushDialog(new MessageBoxOkDlg("The settings.json file contains your progress data. You can copy it to another device or share it with others."));
			#end
			#if js
			// Serialize Settings to JSON
			var localStorage = js.Browser.getLocalStorage();
			if (localStorage != null) {
				var settingsData = localStorage.getItem("MBHaxeSettings");
				if (settingsData != null) {
					// Download this
					var replayBytes = settingsData;
					var blob = new js.html.Blob([haxe.io.Bytes.ofString(replayBytes).getData()], {
						type: 'application/octet-stream'
					});
					var url = js.html.URL.createObjectURL(blob);
					var fname = 'settings.json';
					var element = js.Browser.document.createElement('a');
					element.setAttribute('href', url);
					element.setAttribute('download', fname);

					element.style.display = 'none';
					js.Browser.document.body.appendChild(element);

					element.click();

					js.Browser.document.body.removeChild(element);
					js.html.URL.revokeObjectURL(url);
				}
			}
			#end
		});

		var bottomBar = new GuiControl();
		bottomBar.position = new Vector(0, 590);
		bottomBar.extent = new Vector(640, 200);
		bottomBar.horizSizing = Width;
		bottomBar.vertSizing = Bottom;
		innerCtrl.addChild(bottomBar);

		var backButton = new GuiXboxButton("Back", 160);
		backButton.position = new Vector(400, 0);
		backButton.vertSizing = Bottom;
		backButton.horizSizing = Right;
		backButton.gamepadAccelerator = [Settings.gamepadSettings.back];
		backButton.accelerators = [hxd.Key.ESCAPE, hxd.Key.BACKSPACE];
		backButton.pressedAction = (e) -> MarbleGame.canvas.setContent(new OptionsListGui(false));
		bottomBar.addChild(backButton);
	}

	override function onResize(width:Int, height:Int) {
		var offsetX = (width - 1280) / 2;
		var offsetY = (height - 720) / 2;

		var subX = 640 - (width - offsetX) * 640 / width;
		var subY = 480 - (height - offsetY) * 480 / height;
		innerCtrl.position = new Vector(offsetX, offsetY);
		innerCtrl.extent = new Vector(640 - subX, 480 - subY);

		super.onResize(width, height);
	}
}
