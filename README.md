# Nortia - Agenda Personal Android

Una aplicación Android moderna para gestionar tu agenda personal de eventos.

## 🎯 Características

✨ **Gestión de eventos**
- ➕ Agregar nuevos eventos
- 📝 Ver lista de eventos ordenados por fecha
- 🗑️ Eliminar eventos
- 💾 Almacenamiento en base de datos local

📱 **Interfaz moderna**
- Diseño con Jetpack Compose
- Material Design 3
- Tema claro/oscuro automático

## 🛠️ Tecnologías utilizadas

- **Kotlin**: Lenguaje de programación moderno y seguro
- **Jetpack Compose**: UI declarativa y moderna
- **Room Database**: ORM para SQLite con sincronización en tiempo real
- **Material Design 3**: Sistema de diseño actualizado
- **Coroutines**: Programación asíncrona y no bloqueante

## 📂 Estructura del proyecto

```
Nortia/
├── app/
│   └── src/main/
│       ├── kotlin/com/therry/nortia/
│       │   ├── MainActivity.kt          # Actividad principal
│       │   ├── data/
│       │   │   ├── Event.kt            # Modelo de datos
│       │   │   ├── EventDao.kt         # Acceso a datos
│       │   │   └── AppDatabase.kt      # Configuración de BD
│       │   ├── screens/
│       │   │   └── AgendaScreen.kt     # Pantalla principal
│       │   └── ui/theme/
│       │       ├── Theme.kt            # Tema de la app
│       │       └── Type.kt             # Tipografía
│       └── AndroidManifest.xml
├── build.gradle.kts                    # Configuración de Gradle
└── README.md
```

## 🚀 Cómo compilar y ejecutar

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/therry01d/Nortia.git
   cd Nortia
   ```

2. **Abre en Android Studio**
   - Abre Android Studio
   - Selecciona "Open" y elige la carpeta del proyecto

3. **Sincroniza las dependencias**
   - Android Studio descargará automáticamente las dependencias de Gradle

4. **Ejecuta la aplicación**
   - Haz clic en el botón "Run" o presiona `Shift + F10`
   - Selecciona un emulador o dispositivo físico conectado

## 📋 Roadmap futuro

- [ ] Editar eventos existentes
- [ ] Notificaciones de eventos
- [ ] Categorías de eventos
- [ ] Buscar y filtrar eventos
- [ ] Exportar eventos (iCal, PDF)
- [ ] Sincronización en la nube (Firebase)
- [ ] Soporte para múltiples idiomas
- [ ] Widget de eventos en la pantalla de inicio

## 📄 Licencia

MIT - Libre para usar, modificar y distribuir

## 👨‍💻 Autor

Desarrollado por therry01d

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request
