# Island Drop

A 3D third-person battle royale for Android phones. 100 players, four maps, a shrinking zone.
Play solo or in squads of 4 against bots, or host a match over Wi-Fi so friends on the same network can join.

## Get the APK (free, no PC needed)

1. Put this folder in a GitHub repository (it must include the `.github` folder).
2. Every push runs **Build APK** automatically (Actions tab). It takes about 4 minutes.
3. Open the repository's **Releases** and download `IslandDrop.apk` on your phone.
4. Tap it to install. Android asks to allow "Install unknown apps" for your browser the first time.

To rebuild by hand: Actions → Build APK → Run workflow.

## Build on a PC instead

Open the `android` folder in Android Studio and press Run, or from a terminal:

```
cd android
./gradlew assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/app-debug.apk`.

## The lobby

The game opens in a lobby with your character on a beach deck. Change your name by tapping your profile, pick a shirt, pants and cap under **Outfit**, adjust look speed and aim help under **Settings**, choose a mode, and tap **Start**. Every match begins on a small starting island with a countdown, then everyone boards the plane. Matches earn XP and levels, and levels unlock character skins (Commando, Lifeguard, Ninja, Robot, Royal) and gun skins (Camo, Arctic, Ruby, Gold, Neon) under **Skins**.

Maps (pick one on the mode card): **Palm Island** (beach towns and palms), **Dune Canyon** (cactus fields and rock mesas), **Frost Peak** (snowfields, pines and log cabins), **Neon City** (a futuristic night map with glowing buildings, crystal fields and neon spires). Every map mixes building styles (houses, huts, flat-roofed shops, domes, warehouses, A-frame cabins, neon blocks) and has tall landmark towers: lighthouses, water towers, radio masts or neon spires. In Wi-Fi games the host's map is used.

**Bot level** (on the mode card, and in the host's Wi-Fi lobby): Easy, Normal, Hard or Expert. Harder bots react faster, aim better, jump more and land more headshots, and they give more XP (x0.7, x1, x1.35, x1.7).

**Settings** is split into tabs along the top:
- **Graphics:** quality, resolution, view distance, FPS, shadows, field of view, weather and the FPS counter.
- **Audio:** sound on/off and the master, effects, music, voice chat and background volumes.
- **Controls:** look speed, scope look speed, aim assist, a fixed or floating joystick, invert look, showing the left Fire button, vibration and Customize HUD.
- **Game:** crosshair color, reloading by itself, damage numbers, the kill feed and enemy name tags.
- **Other:** reset all settings.

Graphics has Low / Medium / High presets plus resolution, view distance, 30 / 60 / 120 FPS, field of view (Normal, Wide, Ultra wide) and an FPS counter. **Shadows** (Off, Normal, High) make buildings, trees, players and jeeps cast shadows on the ground; Low turns them off. Try Low on older phones. **Customize HUD** (in Settings) lets you drag and resize every button and change their opacity. Tap **Save** to keep your layout, **Exit** to leave without saving, or **Reset** to go back to the default layout.

**In-match screen layout:**
- **Top left:** the minimap, with the area name and zone timer under it. In squads, your team list with colored numbers and health bars goes under that.
- **Next to the map:** buttons for sound on/off, mic, quick chat and emotes, plus the team chat messages.
- **Top right:** revives left, squads, players alive and your kills. Under that are your two gun cards, which show the gun, magazine and spare ammo; tap one to switch.
- **Bottom center:** health and armor bars, your vest, helmet, scope and backpack icons, and the kill feed.
- **Right side:** a big Fire button, Aim, Jump, Crouch, Jetpack and Reload.
- **Reload animation:** the gun tilts, the old magazine drops out, your other hand brings up a new one, and you pull the handle. **Medkit animation:** the gun hangs at your hip while you open and use a first-aid kit, with green sparkles. Everyone in the match sees these.
- **Healing and reloading** show a progress pill under the crosshair with the seconds left. The Medkit and Reload buttons fill up as they work, and the health bar shows how much health is on the way.
- If a button can't do anything right now (full health, no medkits, full magazine, jetpack recharging, no gloo walls), a short message says why.
- **Left side:** the joystick, Medkit, Gloo wall and a second Fire button.

Loot includes level 1–3 vests and helmets (shown on your character and as icons next to your health) and 2x / 4x scopes (tap **Aim**, or right-click / V). **Backpacks** (level 1–3) let you carry more: ammo 120 → 360, medkits 3 → 9, gloo walls 4 → 10. Movement has momentum: you speed up and slow down smoothly, run slower uphill and faster downhill, have little control in the air, and crouching while running does a slide. After jumping from the plane you ride a **skyboard**: steer it anywhere, tap **Dive** to drop faster, and you dive automatically near the ground. Every town's name floats over the map and shows on the big minimap in the plane, and the area you are in shows under the zone timer. In the plane you can swipe to look around; the top of the screen shows how many players are still in the plane, and an altitude meter shows your height during the flight and drop. **Aim assist** (Settings) is Light by default: a gentle pull while firing with a small deadzone so it never locks on, plus slightly slower turning over a target. Choose Off, Light or Strong.

Every player has a built-in **jetpack**: tap **Jet** (Shift) to dash a long way through the air, then wait for the cooldown ring. Find **Range** (orange canister) and **Cooldown** (blue canister) upgrades on the map, up to level 3 each: dash distance goes from 260 to 575, and cooldown from 10 s down to 3.4 s. **Gloo walls** (icy grenades, 2 per pickup, carry up to 6) put up an ice wall in front of you that blocks bullets until it breaks or melts after 32 s: tap **Wall** (G). **Crouch** (C) makes you smaller and steadier but slower. The plane now flies much higher, so the parachute drop takes longer and you can glide further.

You can land on and walk across rooftops (flat, sloped and curved), jump on them and drop off the edge. Jeeps are parked near every town: walk up and tap **Drive** (F on a keyboard). Each map has its own terrain: hills, dunes or mountains.

**Squad mode** (on the mode card: Solo or Squad): you play in a team of 4 with bot teammates, who follow you off the plane, fight with you and revive you. A squad member who takes a lethal hit is **knocked down** instead of dying: they crawl slowly and bleed out over about 30 seconds unless a teammate stands next to them and taps **Revive** for 4 seconds. The last squad standing wins. Teammates show blue name tags and blue dots on the minimap.

**Revives** are free: every player gets **3 hearts per match** (the ♥ number at the top right). When you are taken out with a heart left, an 8-second countdown starts with a green **♥ Revive** button. Tap it to spend a heart and get back up with 50 HP. If you don't tap it before the timer ends, you are out. While the countdown runs you can crawl slowly, and enemies can still shoot you to finish you off. In squads, a knocked player can still be revived by a teammate first; your own revives are used when no teammate saves you.

**Store and coins**: every match pays coins (more for kills, placing high and winning). Spend them in the lobby **Store** on character models (Ghillie 450, Astronaut 500, Pirate 600, Samurai 750, Cyborg 900, Knight 1000) and gun skins (Dragon 600, Galaxy 800). You start with 300 coins.

## Emotes, quick chat and voice chat

- **Emotes**: tap the smiley button (or T) and pick Wave, Dance, Cheer, Laugh, Thumbs up or Cool. Your character acts it out with an emoji over its head, and everyone sees it. Moving, shooting or getting hit stops the emote.
- **Quick chat** (same panel): Enemy ahead!, Need help!, Follow me, On my way, Need medkit, Need ammo, Nice shot!, Thanks!. In squads only your team sees them, as a bubble over your head and a blue line in the feed. Bot teammates answer and follow you when you ask for help.
- **Voice chat** (Wi-Fi games): tap the mic button in the match (or B), or **Mic** in the Wi-Fi lobby. The first time, Android asks to allow the microphone. Your voice is only sent while you are talking. In squads only your team hears you; in solo Wi-Fi games and in the lobby everyone does. A 🎙 name tag shows who is talking, and **Settings → Voice chat** sets how loud friends are. Voice works in the app. A friend who joined from a browser by typing the address can hear you but can't talk, because browsers only allow the mic on secure pages.

The plane flies at **1 km** and the island is bigger. Loot includes the **LMG** (60-round magazine) and the **Sniper** (6x scope view), plus 10 more guns: **Revolver**, **Micro SMG**, **Carbine**, **Battle Rifle**, **Double Barrel** and **Auto Shotgun** (both shotguns), **Crossbow** (one powerful bolt, 2.5x view) and **Plasma Rifle**. The **Minigun** (120 rounds) and **Heavy Sniper** (8x view) come only from supply drops, and **supply drops** fall inside the zone during the match: watch for the orange smoke and the siren, and open the crate for top-tier gear.

## Sound and effects

Everything is synthesized in the game, so it needs no internet or sound files.

- **Guns** each have their own sound, with an echo for the marksman and sniper. Shots are panned left and right by where they come from and sound muffled far away.
- **Footsteps** change with the ground: grass, sand, snow, metal, or wood floors inside buildings.
- **Other sounds:** reload clicks, an empty-gun click, jump and landing thuds, the jetpack burst, gloo walls freezing and shattering, vehicle engines, the plane drone, and wind while you skyboard (louder when diving).
- **Alerts:** hit ticks and a headshot ding, kill and knock confirms, a revive chime, heal sounds, the zone alarm and an outside-zone beep, the supply-drop siren, a low-health heartbeat, and the countdown beeps.
- **Ambience:** waves and birds on Palm Island, desert and mountain wind, the Neon City hum, plus rain with thunder.
- **Music:** soft music plays in the lobby, with a short tune when you win or lose.
- **Weather:** snow on Frost Peak, blowing sand on Dune Canyon, fireflies in Neon City, and sometimes rain with lightning on Palm Island.
- **Visual effects:** shell casings, bullet-impact dust, footstep and landing dust, a jetpack flame and smoke trail, vehicle dust and exhaust, explosions with smoke, debris and screen shake, and orange smoke on supply drops.
- **Screen effects:** red markers that point to where you were shot from, kill banners (DOUBLE KILL, TRIPLE KILL and more), a pulsing red edge at low health, a purple edge outside the zone, and speed lines when diving, dashing or sliding.

**Settings** has Sound On/Off, Master volume, Lobby music, Game effects, Voice chat, Background sounds (waves, wind and rain; turn it down if you hear a hiss) and a Weather switch. The Low graphics preset shows less weather.

## Playing over Wi-Fi

- Everyone needs to be on the same Wi-Fi, or connected to the host phone's hotspot.
- **Host:** in the lobby tap the mode card, choose **Wi-Fi · Host**, then **Start**. The next screen shows this phone's address.
- **Join:** choose **Wi-Fi · Join**, tap **Start**, and pick the host from the list, or type the address shown on the host's screen. You keep your own name, skins and coins.
- Friends without the app can join from Chrome by opening `http://<host address>:8765`.
- The host chooses whether bots fill the island up to 100 players, then taps **Start match**.
- The host phone runs the match, so it should stay on the game screen until everyone is done.

## How it works

- `android/app/src/main/assets/www/index.html` is the whole game: its own small WebGL engine, so it needs no internet.
- `LanServer.java` runs inside every copy of the app on port 8765. It serves the game page, relays
  WebSocket messages between the host and its guests, and announces the host on UDP port 8766 so
  other phones can find it.
- The host's phone simulates the match. Guests send their controls about 30 times a second and get
  the game state back 15 times a second.

## Controls

| Action | Touch | Keyboard / mouse |
| --- | --- | --- |
| Move | Drag on the left side | WASD |
| Look | Drag on the right side, or drag while holding Fire | Mouse |
| Shoot | Hold Fire (aim helps on touch) | Click |
| Swap gun on the ground | Swap button | E |
| Reload / heal | Buttons | R / Q |
| Jump | Jump button | Space |
| Jetpack dash | Jet button | Shift |
| Crouch | Crouch button | C |
| Gloo wall | Wall button | G |
| Aim / scope | Aim button | Right mouse / V |
| Drive / exit vehicle | Drive button | F |
| Emotes and quick chat | Smiley button | T |
| Voice chat on / off | Mic button | B |
| Jump from the plane / Dive | Jump / Dive button | Space |
