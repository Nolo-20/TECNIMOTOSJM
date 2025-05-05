# Como usarlo
## Requisitos
- Tener bases de datos instalados como XAMPP o Mysql server
- Tener JDK 21 o superior

## Instrucciones
- Importar el proyecto al IDE netbeans o intellij Idea
- Si usas XAMPP abre el archivo "database" y pegalo en el phpmyadmin con eso ya tienes la base de datos completa, configura el archivo conexion que esta en el paquete config y cambiar la variable clave y dejarlo : ""
- Si usas Mysql Server, cuando haces la instalacion de esta te pedira que pongas una contrasena, sugiero que pongas "123456" o cualquier para despues configurarlo en el archivo conexion, para la interfaz usaras mysql workbench para pegar lo que esta en el archivo "database" y listo

## importante
si vas a ejecutar el programa siempre tener encendido el xampp para ejecutar el programa ya que siempre te saldra el error de la base de datos

## Archivo Store
El archivo store es un empaquetado completo de .jar a comparacion con la carpeta "dist" que esta las librerias por aparte, pero el store esta todo, sin preocuparse de que no se pueda ejecutar junto con las libreria
