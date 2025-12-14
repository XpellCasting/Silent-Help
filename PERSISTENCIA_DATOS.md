# Sistema de Persistencia de Datos - SilentHelp

## 📋 Descripción General

La aplicación SilentHelp ahora cuenta con un sistema completo de persistencia de datos que garantiza que el usuario solo necesite registrarse una vez. Los datos se mantienen guardados incluso después de cerrar la aplicación.

## 🔐 Datos que se Guardan

La aplicación guarda de forma segura los siguientes datos del usuario:

- **Estado de registro**: Indica si el usuario completó el registro
- **ID de usuario**: Identificador único del usuario (si el backend lo proporciona)
- **Nombre completo**: Nombre del usuario
- **Teléfono**: Número de teléfono
- **Email**: Correo electrónico
- **Contacto de emergencia**: Nombre y teléfono del contacto de emergencia
- **Fecha de registro**: Momento en que se completó el registro

## 🔄 Flujo de la Aplicación

### Primera vez (Usuario no registrado)
1. Usuario abre la app → Ve pantalla de registro (RegisterStep1Activity)
2. Usuario completa los 3 pasos del registro
3. Datos se guardan automáticamente en SharedPreferences
4. Usuario es redirigido a la pantalla principal (HomeActivity)

### Usos posteriores (Usuario ya registrado)
1. Usuario abre la app → RegisterStep1Activity verifica si está registrado
2. Si está registrado → Redirige automáticamente a HomeActivity
3. Los datos del usuario están disponibles en toda la app

## 📁 Clase Principal: UserPreferences

La clase `UserPreferences.kt` centraliza toda la gestión de datos persistentes. Ubicación:
```
app/src/main/java/com/icc/silent_help/utils/UserPreferences.kt
```

### Métodos Principales

#### Verificar si está registrado
```kotlin
UserPreferences.isUserRegistered(context)
```

#### Guardar datos del usuario
```kotlin
UserPreferences.saveUserData(
    context = this,
    userId = "12345",
    name = "Juan Pérez",
    phone = "+1234567890",
    email = "juan@example.com",
    emergencyContactName = "María Pérez",
    emergencyContactPhone = "+0987654321"
)
```

#### Obtener datos guardados
```kotlin
val userName = UserPreferences.getUserName(context)
val userPhone = UserPreferences.getUserPhone(context)
val userEmail = UserPreferences.getUserEmail(context)
val emergencyName = UserPreferences.getEmergencyContactName(context)
val emergencyPhone = UserPreferences.getEmergencyContactPhone(context)
```

#### Cerrar sesión (borrar todos los datos)
```kotlin
UserPreferences.clearUserData(context)
```

## 🔧 Implementación en las Actividades

### RegisterStep1Activity
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Verificar si ya está registrado
    if (UserPreferences.isUserRegistered(this)) {
        // Ir directamente a HomeActivity
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        return
    }
    
    // Si no está registrado, continuar con el flujo normal
    // ...
}
```

### RegisterStep3Activity
```kotlin
// Cuando el registro se completa exitosamente
UserPreferences.saveUserData(
    context = this,
    userId = userId,
    name = nombre,
    phone = telefono,
    email = email,
    emergencyContactName = nombreEmergencia,
    emergencyContactPhone = telefonoEmergencia
)
```

### HomeActivity
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Verificar que el usuario esté registrado
    if (!UserPreferences.isUserRegistered(this)) {
        // Si no está registrado, redirigir al registro
        val intent = Intent(this, RegisterStep1Activity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        return
    }
    
    // Continuar normalmente...
}
```

### HomeFragment
```kotlin
// Obtener y mostrar el contacto de emergencia guardado
val emergencyName = UserPreferences.getEmergencyContactName(requireContext())
val emergencyPhone = UserPreferences.getEmergencyContactPhone(requireContext())

if (!emergencyName.isNullOrEmpty() && !emergencyPhone.isNullOrEmpty()) {
    // Mostrar el contacto en la lista
}
```

## 🚪 Cerrar Sesión

Los usuarios pueden cerrar sesión desde la sección de **Biometría** en la app:

1. Ir a la pestaña de Biometría
2. Hacer clic en el botón "Cerrar sesión y borrar datos"
3. Confirmar la acción
4. Todos los datos se borrarán y el usuario será redirigido al registro

Esto es útil para:
- Pruebas durante el desarrollo
- Cambiar de cuenta de usuario
- Resetear la aplicación

## 💾 Tecnología Utilizada

La persistencia se implementa usando **SharedPreferences** de Android:
- Almacenamiento local seguro
- Los datos persisten incluso después de cerrar la app
- Los datos se mantienen hasta que se desinstala la app o se llama a `clearUserData()`
- Acceso rápido y eficiente

## 🔒 Seguridad

Los datos se guardan en el almacenamiento privado de la aplicación:
- Solo la app puede acceder a estos datos
- Los datos se guardan en modo `MODE_PRIVATE`
- Para mayor seguridad en producción, considera encriptar datos sensibles

## ⚙️ Archivo de Configuración

Los datos se guardan en:
```
Nombre del archivo: UserPrefs
Ubicación: /data/data/com.icc.silent_help/shared_prefs/UserPrefs.xml
```

## 🧪 Testing

Para probar el flujo completo:

1. **Primera instalación**: 
   - Desinstala la app
   - Instala nuevamente
   - Verifica que aparezca el registro

2. **Usuario registrado**:
   - Completa el registro
   - Cierra la app
   - Abre la app nuevamente
   - Verifica que vaya directo a HomeActivity

3. **Cerrar sesión**:
   - Ve a Biometría
   - Haz clic en "Cerrar sesión"
   - Verifica que redirige al registro
   - Verifica que los datos se hayan borrado

## 📝 Notas Adicionales

- Los datos persisten automáticamente, no requiere acción del usuario
- El sistema es transparente para el usuario final
- Fácil de mantener y extender con nuevos campos
- Código centralizado en una sola clase de utilidad
