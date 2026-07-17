from PIL import Image

# Cargar la imagen
img_path = r"D:\Controlmedicamentos\app\src\main\res\drawable\logo_g7.png"
img = Image.open(img_path)

# Obtener dimensiones actuales
width, height = img.size

# Calcular nuevas dimensiones (al doble)
new_width = width * 2
new_height = height * 2

# Redimensionar la imagen al doble
resized_img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

# Guardar la imagen redimensionada
output_path = r"D:\Controlmedicamentos\app\src\main\res\drawable\logo_g7.png"
resized_img.save(output_path)

print(f"Logo redimensionado al doble: {new_width}x{new_height}")
print(f"Guardado en: {output_path}")
