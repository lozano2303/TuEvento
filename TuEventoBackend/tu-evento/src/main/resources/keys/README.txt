Coloca aquí jwt-private.pem (PKCS#8) para firmar los JWT en local; está en .gitignore.
Generar: openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
Sin este archivo, el perfil dev usa un par de claves en memoria.
