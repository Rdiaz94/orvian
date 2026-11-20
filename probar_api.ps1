# Prueba de humo de la API de Orvian. Usa solo cuentas de prueba locales; nunca pongas aqui contrasenas reales.
# Direccion base de la API
$base = 'http://localhost:8080'

# Llama a la API y devuelve el estado HTTP y el cuerpo, incluso cuando hay error
function Llamar($metodo, $ruta, $token, $cuerpo, $tipo = 'application/json') {
    # Cabeceras de la peticion
    $cabeceras = @{}
    # Si hay token, se envia como Bearer
    if ($token) { $cabeceras['Authorization'] = "Bearer $token" }
    # Parametros base de la peticion
    $parametros = @{ Method = $metodo; Uri = "$base$ruta"; Headers = $cabeceras; UseBasicParsing = $true }
    # Si hay cuerpo, se envia como JSON codificado en UTF-8
    if ($cuerpo) { $parametros['Body'] = [System.Text.Encoding]::UTF8.GetBytes($cuerpo); $parametros['ContentType'] = $tipo }
    # Intenta hacer la llamada
    try {
        # Ejecuta la peticion
        $r = Invoke-WebRequest @parametros
        # Devuelve el estado y el contenido
        return @{ Estado = [int]$r.StatusCode; Cuerpo = $r.Content }
    # Si la API responde con un error HTTP
    } catch {
        # Obtiene la respuesta de error
        $resp = $_.Exception.Response
        # Si hubo respuesta, devuelve su estado
        if ($resp) { return @{ Estado = [int]$resp.StatusCode; Cuerpo = '' } }
        # Si no hubo respuesta, la API no esta disponible
        throw 'No se pudo conectar con la API: esta corriendo mvn spring-boot:run?'
    }
}

# Inicia sesion y devuelve el token
function Token($correo, $clave) {
    # Llama al login con las credenciales
    $r = Llamar 'POST' '/api/auth/login' $null (@{ correo = $correo; contrasena = $clave } | ConvertTo-Json)
    # Devuelve el token de la respuesta
    return ($r.Cuerpo | ConvertFrom-Json).token
}

# Contadores de resultados
$global:ok = 0; $global:falla = 0
# Compara el resultado obtenido con los esperados e imprime si la prueba paso
function Comprobar($descripcion, $esperados, $real) {
    # Si el resultado real esta entre los esperados, la prueba pasa
    if ($esperados -contains $real) { Write-Host "[OK]    $descripcion (resultado $real)" -ForegroundColor Green; $global:ok++ }
    # Si no, la prueba falla
    else { Write-Host "[FALLA] $descripcion (esperado $($esperados -join ' o '), obtenido $real)" -ForegroundColor Red; $global:falla++ }
}

# Tokens de las dos cuentas de prueba
$admin = Token 'ana@correo.com' 'Clave-123456'
# Token del estudiante
$estudiante = Token 'luis@correo.com' 'Clave-654321'

# ===== Paso 7.1: cursos =====
# Sin token no se puede consultar
Comprobar 'Listar cursos sin token' @(401) (Llamar 'GET' '/api/cursos' $null $null).Estado
# Un estudiante no puede crear cursos
Comprobar 'Estudiante crea curso' @(403) (Llamar 'POST' '/api/admin/cursos' $estudiante '{"nombre":"Curso Prohibido"}').Estado
# El administrador crea un curso; si ya existia de una corrida anterior, responde conflicto
Comprobar 'Admin crea curso' @(201, 409) (Llamar 'POST' '/api/admin/cursos' $admin '{"nombre":"Soluciones Web y Aplicaciones Distribuidas"}').Estado
# Un segundo curso para tener datos
Comprobar 'Admin crea otro curso' @(201, 409) (Llamar 'POST' '/api/admin/cursos' $admin '{"nombre":"Base de Datos"}').Estado
# El mismo nombre en minusculas debe rechazarse
Comprobar 'Curso repetido (otra capitalizacion)' @(409) (Llamar 'POST' '/api/admin/cursos' $admin '{"nombre":"base de datos"}').Estado
# Un nombre en blanco es invalido
Comprobar 'Curso con nombre en blanco' @(400) (Llamar 'POST' '/api/admin/cursos' $admin '{"nombre":"   "}').Estado
# Un estudiante si puede consultar los cursos
$lista = Llamar 'GET' '/api/cursos' $estudiante $null
# Comprueba el estado de la consulta
Comprobar 'Estudiante lista cursos' @(200) $lista.Estado
# Toma el identificador del primer curso para usarlo en las tareas
$cursoId = ($lista.Cuerpo | ConvertFrom-Json)[0].id

# ===== Paso 7.2: tareas =====
# Sin token no se puede crear una tarea
Comprobar 'Crear tarea sin token' @(401) (Llamar 'POST' '/api/tareas' $null '{"titulo":"X"}').Estado
# Cuerpo de una tarea valida con espacios sobrantes en el titulo
$cuerpoTarea = '{"titulo":"  Capitulo 1 del proyecto  ","descripcion":"Redactar","cursoId":' + $cursoId + ',"etiqueta":"PROYECTO","fechaVencimiento":"2030-01-01T23:59:00Z"}'
# El estudiante crea su tarea
$creada = Llamar 'POST' '/api/tareas' $estudiante $cuerpoTarea
# Comprueba la creacion
Comprobar 'Estudiante crea tarea' @(201) $creada.Estado
# Convierte la respuesta en objeto
$tarea = $creada.Cuerpo | ConvertFrom-Json
# Identificador de la tarea creada
$id = $tarea.id
# El titulo debe guardarse sin espacios sobrantes
Comprobar 'Titulo sin espacios sobrantes' @($true) ($tarea.titulo -eq 'Capitulo 1 del proyecto')
# Debe empezar pendiente y sin avance
Comprobar 'Empieza pendiente y en 0 %' @($true) (($tarea.estado -eq 'PENDIENTE') -and ($tarea.progreso -eq 0))
# Un titulo en blanco es invalido
Comprobar 'Tarea con titulo en blanco' @(400) (Llamar 'POST' '/api/tareas' $estudiante '{"titulo":"  "}').Estado
# Un curso inexistente es invalido
Comprobar 'Tarea con curso inexistente' @(400) (Llamar 'POST' '/api/tareas' $estudiante '{"titulo":"X","cursoId":999999}').Estado
# Un JSON mal formado es invalido
Comprobar 'Tarea con JSON mal formado' @(400) (Llamar 'POST' '/api/tareas' $estudiante '{"titulo":').Estado
# El dueno puede ver su tarea
Comprobar 'Estudiante ve su tarea' @(200) (Llamar 'GET' "/api/tareas/$id" $estudiante $null).Estado

# --- Seguridad: otro usuario (aunque sea administrador) no puede tocar tareas ajenas ---
# Ver una tarea ajena responde como si no existiera
Comprobar 'Ajeno ve la tarea' @(404) (Llamar 'GET' "/api/tareas/$id" $admin $null).Estado
# Editar una tarea ajena responde como si no existiera
Comprobar 'Ajeno edita la tarea' @(404) (Llamar 'PUT' "/api/tareas/$id" $admin $cuerpoTarea).Estado
# Cambiar el avance de una tarea ajena responde como si no existiera
Comprobar 'Ajeno cambia el avance' @(404) (Llamar 'PATCH' "/api/tareas/$id/avance" $admin '{"estado":"HECHA"}').Estado
# Borrar una tarea ajena responde como si no existiera
Comprobar 'Ajeno borra la tarea' @(404) (Llamar 'DELETE' "/api/tareas/$id" $admin $null).Estado
# La lista del dueno contiene la tarea
$listaDueno = Llamar 'GET' '/api/tareas' $estudiante $null
# Comprueba que la lista incluye la tarea creada
Comprobar 'La lista del dueno incluye su tarea' @($true) ((($listaDueno.Cuerpo | ConvertFrom-Json).content.id) -contains $id)
# La lista del otro usuario no debe contenerla
$listaAjena = Llamar 'GET' '/api/tareas' $admin $null
# Comprueba que la lista ajena no incluye la tarea
Comprobar 'La lista de otro usuario NO la incluye' @($false) ((($listaAjena.Cuerpo | ConvertFrom-Json).content.id) -contains $id)

# --- Edicion y avance ---
# El dueno edita el titulo
Comprobar 'Estudiante edita su tarea' @(200) (Llamar 'PUT' "/api/tareas/$id" $estudiante ('{"titulo":"Capitulo 1 corregido","cursoId":' + $cursoId + '}')).Estado
# Marcar como hecha fija el avance en 100
$hecha = Llamar 'PATCH' "/api/tareas/$id/avance" $estudiante '{"estado":"HECHA"}'
# Comprueba el estado de la respuesta
Comprobar 'Marcar como hecha' @(200) $hecha.Estado
# Comprueba que el avance quedo en 100
Comprobar 'Hecha implica 100 %' @($true) (($hecha.Cuerpo | ConvertFrom-Json).progreso -eq 100)
# En progreso sin avance es invalido
Comprobar 'En progreso sin avance' @(400) (Llamar 'PATCH' "/api/tareas/$id/avance" $estudiante '{"estado":"EN_PROGRESO"}').Estado
# En progreso con avance valido
$enProgreso = Llamar 'PATCH' "/api/tareas/$id/avance" $estudiante '{"estado":"EN_PROGRESO","progreso":40}'
# Comprueba que el avance quedo en 40
Comprobar 'En progreso con 40 %' @($true) (($enProgreso.Cuerpo | ConvertFrom-Json).progreso -eq 40)
# El filtro por estado lista la tarea
$filtrada = Llamar 'GET' '/api/tareas?estado=EN_PROGRESO' $estudiante $null
# Comprueba que el filtro la incluye
Comprobar 'Filtro por estado incluye la tarea' @($true) ((($filtrada.Cuerpo | ConvertFrom-Json).content.id) -contains $id)
# Un estado invalido en el filtro es un error del cliente
Comprobar 'Filtro con estado invalido' @(400) (Llamar 'GET' '/api/tareas?estado=INVENTADO' $estudiante $null).Estado

# --- Limpieza ---
# El dueno borra su tarea de prueba
Comprobar 'Estudiante borra su tarea' @(204) (Llamar 'DELETE' "/api/tareas/$id" $estudiante $null).Estado
# Despues de borrar ya no existe
Comprobar 'La tarea borrada ya no existe' @(404) (Llamar 'GET' "/api/tareas/$id" $estudiante $null).Estado

# ===== Paso 7.3: grupos =====
# Sin token no se puede crear un grupo
Comprobar 'Crear grupo sin token' @(401) (Llamar 'POST' '/api/grupos' $null '{"nombre":"X"}').Estado
# Un nombre en blanco es invalido
Comprobar 'Grupo con nombre en blanco' @(400) (Llamar 'POST' '/api/grupos' $estudiante '{"nombre":"  "}').Estado
# Un curso inexistente es invalido
Comprobar 'Grupo con curso inexistente' @(400) (Llamar 'POST' '/api/grupos' $estudiante '{"nombre":"X","cursoId":999999}').Estado
# El estudiante crea su grupo y queda como lider
$grupoCreado = Llamar 'POST' '/api/grupos' $estudiante ('{"nombre":"  Grupo de prueba  ","descripcion":"Proyecto final","cursoId":' + $cursoId + '}')
# Comprueba la creacion
Comprobar 'Estudiante crea grupo' @(201) $grupoCreado.Estado
# Convierte la respuesta en objeto
$grupo = $grupoCreado.Cuerpo | ConvertFrom-Json
# Identificador del grupo
$gid = $grupo.id
# El creador debe ser lider y el nombre debe venir limpio
Comprobar 'Creador es lider y nombre limpio' @($true) (($grupo.miRol -eq 'LIDER') -and ($grupo.nombre -eq 'Grupo de prueba'))
# El lider ve el detalle y sus integrantes
$detalle = Llamar 'GET' "/api/grupos/$gid" $estudiante $null
# Comprueba el estado del detalle
Comprobar 'Lider ve el grupo' @(200) $detalle.Estado
# Identificador del lider tomado del detalle
$liderId = (($detalle.Cuerpo | ConvertFrom-Json).integrantes)[0].usuarioId
# Un usuario que no pertenece no debe ver el grupo
Comprobar 'No integrante ve el grupo' @(404) (Llamar 'GET' "/api/grupos/$gid" $admin $null).Estado
# La lista del lider incluye el grupo
$listaGrupos = Llamar 'GET' '/api/grupos' $estudiante $null
# Comprueba que la lista lo incluye
Comprobar 'Lista del lider incluye el grupo' @($true) ((($listaGrupos.Cuerpo | ConvertFrom-Json).content.id) -contains $gid)
# La lista de quien no pertenece no lo incluye
$listaAjena = Llamar 'GET' '/api/grupos' $admin $null
# Comprueba que la lista ajena no lo incluye
Comprobar 'Lista de un ajeno NO incluye el grupo' @($false) ((($listaAjena.Cuerpo | ConvertFrom-Json).content.id) -contains $gid)
# Un no integrante no puede invitarse ni invitar a otros
Comprobar 'No integrante agrega integrantes' @(404) (Llamar 'POST' "/api/grupos/$gid/integrantes" $admin '{"correo":"ana@correo.com"}').Estado
# Invitar a un correo inexistente
Comprobar 'Invitar a un correo inexistente' @(404) (Llamar 'POST' "/api/grupos/$gid/integrantes" $estudiante '{"correo":"nadie@correo.com"}').Estado
# El lider invita a Ana, escribiendo el correo en mayusculas
$invitacion = Llamar 'POST' "/api/grupos/$gid/integrantes" $estudiante '{"correo":" ANA@correo.com "}'
# Comprueba la invitacion
Comprobar 'Lider invita a un integrante' @(201) $invitacion.Estado
# Identificador de Ana
$anaId = ($invitacion.Cuerpo | ConvertFrom-Json).usuarioId
# Invitarla otra vez es un conflicto
Comprobar 'Invitar a alguien que ya es integrante' @(409) (Llamar 'POST' "/api/grupos/$gid/integrantes" $estudiante '{"correo":"ana@correo.com"}').Estado
# Ahora Ana, ya integrante, ve el grupo
Comprobar 'Integrante ve el grupo' @(200) (Llamar 'GET' "/api/grupos/$gid" $admin $null).Estado
# El detalle no debe exponer correos de los integrantes
Comprobar 'El detalle no expone correos' @($false) ((Llamar 'GET' "/api/grupos/$gid" $admin $null).Cuerpo -match 'correo')
# Un miembro comun no puede invitar
Comprobar 'Miembro comun invita' @(403) (Llamar 'POST' "/api/grupos/$gid/integrantes" $admin '{"correo":"luis@correo.com"}').Estado
# Un miembro comun no puede expulsar al lider
Comprobar 'Miembro comun expulsa al lider' @(403) (Llamar 'DELETE' "/api/grupos/$gid/integrantes/$liderId" $admin $null).Estado
# Un miembro comun no puede eliminar el grupo
Comprobar 'Miembro comun elimina el grupo' @(403) (Llamar 'DELETE' "/api/grupos/$gid" $admin $null).Estado
# El lider no puede abandonar el grupo
Comprobar 'Lider abandona su grupo' @(400) (Llamar 'DELETE' "/api/grupos/$gid/integrantes/$liderId" $estudiante $null).Estado
# El lider quita a Ana
Comprobar 'Lider quita a un integrante' @(204) (Llamar 'DELETE' "/api/grupos/$gid/integrantes/$anaId" $estudiante $null).Estado
# Ana ya no ve el grupo
Comprobar 'Ex integrante ya no ve el grupo' @(404) (Llamar 'GET' "/api/grupos/$gid" $admin $null).Estado
# El lider la invita de nuevo para probar que ella puede irse sola
Comprobar 'Lider vuelve a invitar' @(201) (Llamar 'POST' "/api/grupos/$gid/integrantes" $estudiante '{"correo":"ana@correo.com"}').Estado
# Ana abandona el grupo por su cuenta
Comprobar 'Miembro abandona el grupo' @(204) (Llamar 'DELETE' "/api/grupos/$gid/integrantes/$anaId" $admin $null).Estado
# El lider elimina el grupo y limpia la prueba
Comprobar 'Lider elimina el grupo' @(204) (Llamar 'DELETE' "/api/grupos/$gid" $estudiante $null).Estado
# El grupo eliminado ya no existe
Comprobar 'El grupo eliminado ya no existe' @(404) (Llamar 'GET' "/api/grupos/$gid" $estudiante $null).Estado

# ===== Paso 7.4: tareas de grupo =====
# El estudiante (futuro lider) crea un grupo para las pruebas
$g4 = (Llamar 'POST' '/api/grupos' $estudiante ('{"nombre":"Grupo de tareas","cursoId":' + $cursoId + '}')).Cuerpo | ConvertFrom-Json
# Identificador del grupo
$g4id = $g4.id
# Quien no pertenece no puede listar las tareas del grupo
Comprobar 'No integrante lista tareas de grupo' @(404) (Llamar 'GET' "/api/grupos/$g4id/tareas" $admin $null).Estado
# Quien no pertenece no puede crear tareas en el grupo
Comprobar 'No integrante crea tarea de grupo' @(404) (Llamar 'POST' "/api/grupos/$g4id/tareas" $admin '{"titulo":"X"}').Estado
# El lider crea una tarea para si mismo
$t1r = Llamar 'POST' "/api/grupos/$g4id/tareas" $estudiante '{"titulo":"  Tarea del lider  ","etiqueta":"PROYECTO"}'
# Comprueba la creacion
Comprobar 'Lider crea tarea de grupo' @(201) $t1r.Estado
# Identificador de la tarea del lider
$t1 = ($t1r.Cuerpo | ConvertFrom-Json).id
# El lider invita a Ana al grupo
Comprobar 'Lider invita a Ana' @(201) (Llamar 'POST' "/api/grupos/$g4id/integrantes" $estudiante '{"correo":"ana@correo.com"}').Estado
# El lider asigna una tarea a Ana
$t2r = Llamar 'POST' "/api/grupos/$g4id/tareas" $estudiante ('{"titulo":"Tarea de Ana","responsableId":' + $anaId + '}')
# Comprueba la creacion
Comprobar 'Lider asigna tarea a un integrante' @(201) $t2r.Estado
# Identificador de la tarea de Ana
$t2 = ($t2r.Cuerpo | ConvertFrom-Json).id
# La respuesta debe indicar a Ana como responsable
Comprobar 'La tarea queda asignada a Ana' @($true) ((($t2r.Cuerpo | ConvertFrom-Json).responsableId) -eq $anaId)
# El lider no puede asignar a alguien que no existe en el grupo
Comprobar 'Asignar a quien no es integrante' @(400) (Llamar 'POST' "/api/grupos/$g4id/tareas" $estudiante '{"titulo":"X","responsableId":999999}').Estado
# Ana, ya integrante, ve las tareas del grupo
$listaG = Llamar 'GET' "/api/grupos/$g4id/tareas" $admin $null
# Comprueba el estado de la lista
Comprobar 'Integrante lista tareas de grupo' @(200) $listaG.Estado
# La lista incluye las dos tareas
Comprobar 'La lista incluye ambas tareas' @($true) ((($listaG.Cuerpo | ConvertFrom-Json).content.id) -contains $t1 -and (($listaG.Cuerpo | ConvertFrom-Json).content.id) -contains $t2)
# La lista personal de Ana incluye solo la tarea asignada a ella
$personalAna = (Llamar 'GET' '/api/tareas' $admin $null).Cuerpo | ConvertFrom-Json
# Comprueba que incluye la suya y no la del lider
Comprobar 'Mis tareas incluye solo la asignada' @($true) (($personalAna.content.id -contains $t2) -and -not ($personalAna.content.id -contains $t1))
# Ana no puede editar una tarea de grupo desde la ruta personal
Comprobar 'Editar tarea de grupo por ruta personal' @(403) (Llamar 'PUT' "/api/tareas/$t2" $admin '{"titulo":"Hackeo"}').Estado
# Ana no puede borrar una tarea de grupo desde la ruta personal
Comprobar 'Borrar tarea de grupo por ruta personal' @(403) (Llamar 'DELETE' "/api/tareas/$t2" $admin $null).Estado
# Ana puede avanzar su propia tarea desde la ruta personal
Comprobar 'Asignada avanza su tarea (ruta personal)' @(200) (Llamar 'PATCH' "/api/tareas/$t2/avance" $admin '{"estado":"EN_PROGRESO","progreso":30}').Estado
# Ana no puede avanzar la tarea del lider
Comprobar 'Miembro avanza tarea ajena de grupo' @(403) (Llamar 'PATCH' "/api/grupos/$g4id/tareas/$t1/avance" $admin '{"estado":"HECHA"}').Estado
# Ana no puede editar tareas del grupo
Comprobar 'Miembro edita tarea de grupo' @(403) (Llamar 'PUT' "/api/grupos/$g4id/tareas/$t2" $admin '{"titulo":"Cambio"}').Estado
# Ana no puede borrar tareas del grupo
Comprobar 'Miembro borra tarea de grupo' @(403) (Llamar 'DELETE' "/api/grupos/$g4id/tareas/$t2" $admin $null).Estado
# Ana no puede asignar tareas a otra persona
Comprobar 'Miembro asigna tarea a otro' @(403) (Llamar 'POST' "/api/grupos/$g4id/tareas" $admin ('{"titulo":"X","responsableId":' + $liderId + '}')).Estado
# Ana puede crear una tarea para si misma
$t3r = Llamar 'POST' "/api/grupos/$g4id/tareas" $admin '{"titulo":"Tarea propia de Ana"}'
# Comprueba la creacion
Comprobar 'Miembro crea tarea para si mismo' @(201) $t3r.Estado
# Identificador de la tarea propia de Ana
$t3 = ($t3r.Cuerpo | ConvertFrom-Json).id
# El lider puede cambiar el avance de la tarea de Ana
Comprobar 'Lider avanza tarea de un integrante' @(200) (Llamar 'PATCH' "/api/grupos/$g4id/tareas/$t2/avance" $estudiante '{"estado":"HECHA"}').Estado
# El lider puede editar una tarea del grupo
Comprobar 'Lider edita tarea de grupo' @(200) (Llamar 'PUT' "/api/grupos/$g4id/tareas/$t2" $estudiante '{"titulo":"Tarea de Ana corregida"}').Estado
# Un segundo grupo del lider para probar que los identificadores no cruzan grupos
$g5id = ((Llamar 'POST' '/api/grupos' $estudiante '{"nombre":"Otro grupo"}').Cuerpo | ConvertFrom-Json).id
# Una tarea del primer grupo no se alcanza usando el segundo grupo
Comprobar 'Tarea de otro grupo (cruce de ids)' @(404) (Llamar 'PATCH' "/api/grupos/$g5id/tareas/$t1/avance" $estudiante '{"estado":"HECHA"}').Estado
# El lider borra una tarea del grupo
Comprobar 'Lider borra tarea de grupo' @(204) (Llamar 'DELETE' "/api/grupos/$g4id/tareas/$t2" $estudiante $null).Estado
# El lider elimina el primer grupo y con el sus tareas
Comprobar 'Lider elimina el grupo' @(204) (Llamar 'DELETE' "/api/grupos/$g4id" $estudiante $null).Estado
# Las tareas del grupo eliminado desaparecen tambien para Ana
Comprobar 'Tareas del grupo eliminado se borran' @(404) (Llamar 'GET' "/api/tareas/$t3" $admin $null).Estado
# Limpieza del segundo grupo
Comprobar 'Lider elimina el segundo grupo' @(204) (Llamar 'DELETE' "/api/grupos/$g5id" $estudiante $null).Estado

# ===== Paso 7.5: recursos =====
# Sin token no se puede consultar la biblioteca
Comprobar 'Listar recursos sin token' @(401) (Llamar 'GET' '/api/recursos' $null $null).Estado
# Un enlace javascript: es peligroso y debe rechazarse
Comprobar 'Recurso con enlace javascript' @(400) (Llamar 'POST' '/api/recursos' $estudiante '{"titulo":"X","tipo":"ENLACE","url":"javascript:alert(1)"}').Estado
# El tipo es obligatorio
Comprobar 'Recurso sin tipo' @(400) (Llamar 'POST' '/api/recursos' $estudiante '{"titulo":"X","url":"https://ejemplo.com"}').Estado
# Un curso inexistente es invalido
Comprobar 'Recurso con curso inexistente' @(400) (Llamar 'POST' '/api/recursos' $estudiante '{"titulo":"X","tipo":"PDF","url":"https://ejemplo.com/x.pdf","cursoId":999999}').Estado
# El lider crea un grupo para los recursos privados
$g6id = ((Llamar 'POST' '/api/grupos' $estudiante '{"nombre":"Grupo de recursos"}').Cuerpo | ConvertFrom-Json).id
# Quien no pertenece al grupo no puede compartir en el
Comprobar 'Compartir en un grupo ajeno' @(404) (Llamar 'POST' '/api/recursos' $admin ('{"titulo":"X","tipo":"PDF","url":"https://ejemplo.com/x.pdf","grupoId":' + $g6id + '}')).Estado
# El estudiante comparte un recurso publico con espacios sobrantes en el titulo
$r1r = Llamar 'POST' '/api/recursos' $estudiante '{"titulo":"  Guia de ejemplo  ","tipo":"PDF","url":"https://ejemplo.com/guia.pdf","descripcion":"Material de apoyo"}'
# Comprueba la creacion
Comprobar 'Estudiante comparte recurso publico' @(201) $r1r.Estado
# Convierte la respuesta en objeto
$r1 = $r1r.Cuerpo | ConvertFrom-Json
# El titulo debe quedar limpio y el creador debe figurar como autor
Comprobar 'Titulo limpio y autor correcto' @($true) (($r1.titulo -eq 'Guia de ejemplo') -and $r1.esAutor)
# El lider comparte un recurso privado en su grupo
$r2r = Llamar 'POST' '/api/recursos' $estudiante ('{"titulo":"Material privado","tipo":"VIDEO","url":"https://ejemplo.com/video","grupoId":' + $g6id + '}')
# Comprueba la creacion
Comprobar 'Lider comparte recurso en su grupo' @(201) $r2r.Estado
# Convierte la respuesta en objeto
$r2 = $r2r.Cuerpo | ConvertFrom-Json
# Lista de Ana, que todavia no pertenece al grupo
$listaA = (Llamar 'GET' '/api/recursos' $admin $null).Cuerpo | ConvertFrom-Json
# Ana ve el recurso publico
Comprobar 'No integrante ve el recurso publico' @($true) ($listaA.content.id -contains $r1.id)
# Ana no ve el recurso del grupo
Comprobar 'No integrante NO ve el de grupo' @($false) ($listaA.content.id -contains $r2.id)
# Ana no puede consultar el recurso del grupo por su identificador
Comprobar 'No integrante consulta el de grupo' @(404) (Llamar 'GET' "/api/recursos/$($r2.id)" $admin $null).Estado
# Ana no puede descargarlo
Comprobar 'No integrante descarga el de grupo' @(404) (Llamar 'POST' "/api/recursos/$($r2.id)/descarga" $admin $null).Estado
# El lider invita a Ana
Comprobar 'Lider invita a Ana' @(201) (Llamar 'POST' "/api/grupos/$g6id/integrantes" $estudiante '{"correo":"ana@correo.com"}').Estado
# Lista de Ana, ya integrante
$listaB = (Llamar 'GET' '/api/recursos' $admin $null).Cuerpo | ConvertFrom-Json
# Ahora si ve el recurso del grupo
Comprobar 'Integrante ahora ve el de grupo' @($true) ($listaB.content.id -contains $r2.id)
# Y puede consultarlo
Comprobar 'Integrante consulta el de grupo' @(200) (Llamar 'GET' "/api/recursos/$($r2.id)" $admin $null).Estado
# Ana descarga el recurso publico
$d1 = Llamar 'POST' "/api/recursos/$($r1.id)/descarga" $admin $null
# Debe responder bien y devolver el enlace
Comprobar 'Descarga devuelve el enlace' @($true) (($d1.Estado -eq 200) -and ((($d1.Cuerpo | ConvertFrom-Json).url) -eq 'https://ejemplo.com/guia.pdf'))
# Segunda descarga
Comprobar 'Segunda descarga' @(200) (Llamar 'POST' "/api/recursos/$($r1.id)/descarga" $admin $null).Estado
# El contador debe haber sumado dos
Comprobar 'El contador suma 2' @($true) ((((Llamar 'GET' "/api/recursos/$($r1.id)" $admin $null).Cuerpo | ConvertFrom-Json).descargas) -eq 2)
# Ordenado por populares, las descargas deben venir de mayor a menor; asi la prueba no depende de otros recursos que haya en la base
$descargasPopulares = @(((Llamar 'GET' '/api/recursos?orden=POPULARES' $admin $null).Cuerpo | ConvertFrom-Json).content.descargas)
# Compara el orden recibido con el orden descendente esperado
Comprobar 'Populares: ordenados de mayor a menor descarga' @($true) ((($descargasPopulares | Sort-Object -Descending) -join ',') -eq ($descargasPopulares -join ','))
# Filtrando por video no aparece el PDF
Comprobar 'Filtro por tipo excluye otros' @($false) ((((Llamar 'GET' '/api/recursos?tipo=VIDEO' $admin $null).Cuerpo | ConvertFrom-Json).content.id) -contains $r1.id)
# La busqueda por texto no distingue mayusculas
Comprobar 'Busqueda por texto encuentra' @($true) ((((Llamar 'GET' '/api/recursos?q=EJEMPLO' $admin $null).Cuerpo | ConvertFrom-Json).content.id) -contains $r1.id)
# Una busqueda sin coincidencias no devuelve el recurso
Comprobar 'Busqueda sin coincidencias' @($false) ((((Llamar 'GET' '/api/recursos?q=zzzz' $admin $null).Cuerpo | ConvertFrom-Json).content.id) -contains $r1.id)
# Un orden inventado es un error del cliente
Comprobar 'Orden invalido' @(400) (Llamar 'GET' '/api/recursos?orden=INVENTADO' $admin $null).Estado
# Quien no es autor no puede editar
Comprobar 'Ajeno edita recurso' @(403) (Llamar 'PUT' "/api/recursos/$($r1.id)" $admin '{"titulo":"Hack","tipo":"PDF","url":"https://ejemplo.com/h.pdf"}').Estado
# El autor si puede editar
Comprobar 'Autor edita su recurso' @(200) (Llamar 'PUT' "/api/recursos/$($r1.id)" $estudiante '{"titulo":"Guia corregida","tipo":"PDF","url":"https://ejemplo.com/guia.pdf"}').Estado
# El autor no puede cambiar el grupo de un recurso
Comprobar 'Autor intenta cambiar el grupo' @(400) (Llamar 'PUT' "/api/recursos/$($r1.id)" $estudiante ('{"titulo":"X","tipo":"PDF","url":"https://ejemplo.com/g.pdf","grupoId":' + $g6id + '}')).Estado
# Un recurso publico ajeno no se puede borrar
Comprobar 'Ajeno borra recurso publico' @(403) (Llamar 'DELETE' "/api/recursos/$($r1.id)" $admin $null).Estado
# Un miembro comun no puede borrar un recurso de grupo ajeno
Comprobar 'Miembro borra recurso ajeno de grupo' @(403) (Llamar 'DELETE' "/api/recursos/$($r2.id)" $admin $null).Estado
# Ana comparte un recurso en el grupo
$r3r = Llamar 'POST' '/api/recursos' $admin ('{"titulo":"Aporte de Ana","tipo":"ENLACE","url":"https://ejemplo.com/aporte","grupoId":' + $g6id + '}')
# Comprueba la creacion
Comprobar 'Miembro comparte en el grupo' @(201) $r3r.Estado
# El lider puede borrar el aporte de Ana
Comprobar 'Lider borra recurso de un miembro' @(204) (Llamar 'DELETE' "/api/recursos/$(($r3r.Cuerpo | ConvertFrom-Json).id)" $estudiante $null).Estado
# El autor borra su recurso publico
Comprobar 'Autor borra su recurso' @(204) (Llamar 'DELETE' "/api/recursos/$($r1.id)" $estudiante $null).Estado
# Despues de borrar ya no existe
Comprobar 'Recurso borrado ya no existe' @(404) (Llamar 'GET' "/api/recursos/$($r1.id)" $estudiante $null).Estado
# El lider elimina el grupo
Comprobar 'Lider elimina el grupo de recursos' @(204) (Llamar 'DELETE' "/api/grupos/$g6id" $estudiante $null).Estado
# Los recursos del grupo se borran con el
Comprobar 'Recursos del grupo eliminado se borran' @(404) (Llamar 'GET' "/api/recursos/$($r2.id)" $estudiante $null).Estado

# ===== Paso 7.7: resumen academico por REST =====
# Sin token no se puede consultar el resumen
Comprobar 'Resumen REST sin token' @(401) (Llamar 'GET' '/api/resumen' $null $null).Estado
# El estudiante consulta su resumen
$restResumen = Llamar 'GET' '/api/resumen' $estudiante $null
# Comprueba el estado
Comprobar 'Resumen REST con token' @(200) $restResumen.Estado
# Conserva el total de tareas del resumen
$totalRest = ($restResumen.Cuerpo | ConvertFrom-Json).tareasTotales
# El estudiante crea una tarea para comprobar que el resumen la cuenta
$tNueva = ((Llamar 'POST' '/api/tareas' $estudiante '{"titulo":"Tarea del resumen"}').Cuerpo | ConvertFrom-Json).id
# El total debe subir en uno
Comprobar 'El resumen cuenta la tarea nueva' @($true) ((((Llamar 'GET' '/api/resumen' $estudiante $null).Cuerpo | ConvertFrom-Json).tareasTotales) -eq ($totalRest + 1))
# Limpieza de la tarea de prueba
Comprobar 'Limpieza de la tarea del resumen' @(204) (Llamar 'DELETE' "/api/tareas/$tNueva" $estudiante $null).Estado

# ===== Paso 7.8: limite de intentos de inicio de sesion =====
# Correo inventado distinto en cada corrida para no dejar bloqueos entre ejecuciones
$falso = "bloqueo-$(Get-Random)@correo.com"
# Cuerpo del inicio de sesion con una contrasena incorrecta
$cuerpoMalo = (@{ correo = $falso; contrasena = 'incorrecta' } | ConvertTo-Json)
# Los primeros cinco intentos fallidos responden credenciales invalidas
foreach ($n in 1..5) { Comprobar "Intento fallido $n" @(401) (Llamar 'POST' '/api/auth/login' $null $cuerpoMalo).Estado }
# El sexto intento ya queda bloqueado
Comprobar 'El sexto intento se bloquea' @(429) (Llamar 'POST' '/api/auth/login' $null $cuerpoMalo).Estado
# Un usuario legitimo, con otro correo, sigue pudiendo entrar
Comprobar 'Un usuario legitimo no se ve afectado' @(200) (Llamar 'POST' '/api/auth/login' $null (@{ correo = 'luis@correo.com'; contrasena = 'Clave-654321' } | ConvertTo-Json)).Estado

# ===== Modulo 9: perfil =====
# Sin token no se puede editar el perfil
Comprobar 'Perfil sin token' @(401) (Llamar 'PUT' '/api/perfil' $null '{"nombre":"X"}').Estado
# Un nombre en blanco es invalido
Comprobar 'Perfil con nombre en blanco' @(400) (Llamar 'PUT' '/api/perfil' $estudiante '{"nombre":"  "}').Estado
# Un ciclo fuera de rango es invalido
Comprobar 'Perfil con ciclo fuera de rango' @(400) (Llamar 'PUT' '/api/perfil' $estudiante '{"nombre":"Luis Rojas","ciclo":99}').Estado
# El estudiante edita su perfil, con espacios sobrantes en el nombre
$edicion = Llamar 'PUT' '/api/perfil' $estudiante '{"nombre":"  Luis Rojas  ","carrera":"Ingenieria de Sistemas Computacionales","ciclo":4}'
# Comprueba el estado
Comprobar 'Estudiante edita su perfil' @(200) $edicion.Estado
# El nombre debe quedar sin espacios sobrantes
Comprobar 'El nombre queda limpio' @($true) ((($edicion.Cuerpo | ConvertFrom-Json).nombre) -eq 'Luis Rojas')
# Sin la contrasena actual correcta no se puede cambiar; responde 400 y no 401 para no cerrar la sesion
Comprobar 'Cambio con la contrasena actual incorrecta' @(400) (Llamar 'PUT' '/api/perfil/contrasena' $estudiante '{"contrasenaActual":"incorrecta","contrasenaNueva":"Clave-nueva-987"}').Estado
# La nueva no puede ser igual a la actual
Comprobar 'Contrasena nueva igual a la actual' @(400) (Llamar 'PUT' '/api/perfil/contrasena' $estudiante '{"contrasenaActual":"Clave-654321","contrasenaNueva":"Clave-654321"}').Estado
# La nueva debe tener al menos 8 caracteres
Comprobar 'Contrasena nueva demasiado corta' @(400) (Llamar 'PUT' '/api/perfil/contrasena' $estudiante '{"contrasenaActual":"Clave-654321","contrasenaNueva":"123"}').Estado
# El estudiante cambia su contrasena
Comprobar 'Estudiante cambia su contrasena' @(204) (Llamar 'PUT' '/api/perfil/contrasena' $estudiante '{"contrasenaActual":"Clave-654321","contrasenaNueva":"Clave-nueva-987"}').Estado
# Con la contrasena nueva se puede entrar
Comprobar 'Entra con la contrasena nueva' @(200) (Llamar 'POST' '/api/auth/login' $null (@{ correo = 'luis@correo.com'; contrasena = 'Clave-nueva-987' } | ConvertTo-Json)).Estado
# La contrasena vieja ya no sirve
Comprobar 'La contrasena vieja ya no sirve' @(401) (Llamar 'POST' '/api/auth/login' $null (@{ correo = 'luis@correo.com'; contrasena = 'Clave-654321' } | ConvertTo-Json)).Estado
# Se restaura la contrasena original para que el script se pueda repetir
Comprobar 'Se restaura la contrasena original' @(204) (Llamar 'PUT' '/api/perfil/contrasena' $estudiante '{"contrasenaActual":"Clave-nueva-987","contrasenaNueva":"Clave-654321"}').Estado
# Con la contrasena original se vuelve a entrar
Comprobar 'Entra otra vez con la original' @(200) (Llamar 'POST' '/api/auth/login' $null (@{ correo = 'luis@correo.com'; contrasena = 'Clave-654321' } | ConvertTo-Json)).Estado

# Resumen final
Write-Host "`nResultado: $global:ok correctas, $global:falla fallidas" -ForegroundColor Cyan
