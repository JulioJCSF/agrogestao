-- A carga inicial rodou com os placeholders do admin sem resolver: o usuário foi gravado com o
-- login literal '$' || '{admin.login}' e a senha literal correspondente, e ninguém conseguia logar.
-- Aqui o admin passa a ter o ADMIN_LOGIN e a ADMIN_SENHA do .env. Em banco novo, nada é alterado.
UPDATE usuario
SET login         = '${ADMIN_LOGIN}',
    senha_hash    = public.crypt('${ADMIN_SENHA}', public.gen_salt('bf', 10)),
    atualizado_em = now()
WHERE login = '$' || '{admin.login}';
