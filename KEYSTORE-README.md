# Keystore de firma release — Nortia

Copia de referencia de `app/nortia-release.jks` (la que realmente usa Gradle
para firmar, referenciada desde `app/build.gradle.kts`). Esta copia en la raíz
es solo para tenerla a la mano; si algún día difieren, la que manda es la de
`app/`.

- **Alias:** `nortia`
- **Contraseña del store:** `nortia2026`
- **Contraseña de la key:** `nortia2026`

Va versionada a propósito (ver excepción en `.gitignore`): es una app personal
instalada fuera de Google Play, y compartir la misma firma entre builds es lo
que permite instalar actualizaciones encima sin desinstalar y perder datos.

Si algún día se publica en una tienda, hay que generar una keystore privada
nueva y sacarla del repositorio.
