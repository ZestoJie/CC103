from PIL import Image, ImageDraw
import math

# Settings Icon - gear/cog style
img_settings = Image.new('RGB', (128, 128), color='white')
draw_settings = ImageDraw.Draw(img_settings)

# Draw a gear/cog
center_x, center_y = 64, 64
outer_radius = 50
inner_radius = 30
tooth_size = 8

# Draw outer circle
draw_settings.ellipse([center_x-outer_radius, center_y-outer_radius, center_x+outer_radius, center_y+outer_radius], outline='black', width=3)

# Draw inner circle
draw_settings.ellipse([center_x-inner_radius, center_y-inner_radius, center_x+inner_radius, center_y+inner_radius], outline='black', width=3)

# Draw teeth (small rectangles)
for i in range(8):
    angle = (i * 2 * math.pi) / 8
    x1 = center_x + (outer_radius + 5) * math.cos(angle)
    y1 = center_y + (outer_radius + 5) * math.sin(angle)
    x2 = center_x + (outer_radius + tooth_size + 5) * math.cos(angle)
    y2 = center_y + (outer_radius + tooth_size + 5) * math.sin(angle)
    draw_settings.line([(x1, y1), (x2, y2)], fill='black', width=4)

img_settings.save('src/main/resources/images/icon-settingsblue.png')
print("Settings icon created")

# Logout Icon - exit/door style
img_logout = Image.new('RGB', (128, 128), color='white')
draw_logout = ImageDraw.Draw(img_logout)

# Draw a door/exit icon
# Door frame
draw_logout.rectangle([20, 30, 80, 110], outline='black', width=3)
# Door handle (circle)
draw_logout.ellipse([65, 65, 75, 75], outline='black', width=2)
# Arrow pointing right (exit)
arrow_x, arrow_y = 90, 70
draw_logout.line([(85, arrow_y), (arrow_x, arrow_y)], fill='black', width=3)
draw_logout.line([(arrow_x, arrow_y), (arrow_x-8, arrow_y-8)], fill='black', width=3)
draw_logout.line([(arrow_x, arrow_y), (arrow_x-8, arrow_y+8)], fill='black', width=3)

img_logout.save('src/main/resources/images/icon-logout.png')
print("Logout icon created")
