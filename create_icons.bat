@echo off
cd /d "c:\Users\Brad\Downloads\CC103-19.95BeforeOpti\CC103-19.95BeforeOpti\src\main\resources\images"

REM Create Settings icon (gear)
convert -size 128x128 xc:white -stroke black -strokewidth 3 ^
-draw "circle 64,64 50,64" ^
-draw "circle 64,64 30,64" ^
-draw "line 64,14 64,10" ^
-draw "line 64,118 64,114" ^
-draw "line 114,64 118,64" ^
-draw "line 10,64 14,64" ^
-draw "line 104,24 107,21" ^
-draw "line 24,104 21,107" ^
-draw "line 104,104 107,107" ^
-draw "line 24,24 21,21" ^
icon-settingsblue.png

REM Create Logout icon (door with arrow)
convert -size 128x128 xc:white -stroke black -strokewidth 3 -fill white ^
-draw "rectangle 20,30 80,110" ^
-draw "circle 70,70 3" ^
-draw "line 85,70 100,70" ^
-draw "line 100,70 92,62" ^
-draw "line 100,70 92,78" ^
icon-logout.png

echo Icons created successfully!
