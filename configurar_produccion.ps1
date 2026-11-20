# Guarda en esta cuenta de Windows las variables de entorno que usa el perfil de produccion; no muestra ningun secreto.
# Pide la contrasena del usuario de la base de datos sin mostrarla en pantalla
$clave = Read-Host 'Contrasena del usuario orvian_app de PostgreSQL' -AsSecureString
# Convierte la contrasena a texto solo para guardarla
$texto = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave))
# Genera un secreto JWT nuevo de 32 bytes aleatorios
$bytes = New-Object byte[] 32
# Generador criptografico de numeros aleatorios
$generador = [Security.Cryptography.RandomNumberGenerator]::Create()
# Llena los bytes con valores aleatorios
$generador.GetBytes($bytes)
# Variables que se guardan, sin ningun valor impreso
$variables = @{
    # Direccion de la base de datos local
    'ORVIAN_DB_URL' = 'jdbc:postgresql://localhost:5432/orvian'
    # Usuario de la aplicacion en PostgreSQL
    'ORVIAN_DB_USUARIO' = 'orvian_app'
    # Contrasena pedida arriba
    'ORVIAN_DB_CLAVE' = $texto
    # Secreto JWT generado arriba, distinto al de desarrollo
    'ORVIAN_JWT_SECRETO' = [Convert]::ToBase64String($bytes)
    # Origen del frontend Angular en desarrollo
    'ORVIAN_CORS_ORIGENES' = 'http://localhost:4200'
}
# Guarda cada variable a nivel de usuario para que Tomcat la herede
foreach ($nombre in $variables.Keys) { [Environment]::SetEnvironmentVariable($nombre, $variables[$nombre], 'User') }
# Confirma sin mostrar valores
Write-Host "Listo: $($variables.Count) variables guardadas. Cierra y vuelve a abrir VS Code para que Tomcat las reciba." -ForegroundColor Green
