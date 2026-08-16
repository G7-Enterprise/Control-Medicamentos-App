@echo off
color 0A
echo ===================================================
echo   Subiendo el APK a GitHub...
echo ===================================================
echo.

:: Navegar a la carpeta del proyecto
cd /d D:\Controlmedicamentos

:: Forzar la adición del archivo APK
echo [1/3] Preparando el archivo...
git add -f app\build\outputs\apk\release\app-release_ambassador.apk

:: Crear el punto de guardado
echo [2/3] Confirmando los cambios...
git commit -m "Actualizacion automatica de APK modificado ambassador"

:: Subir a GitHub
echo [3/3] Subiendo a la nube (esto puede tardar unos segundos)...
git push

echo.
echo ===================================================
echo   ¡Subida completada exitosamente!
echo ===================================================
pause