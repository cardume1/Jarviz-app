# Gerando o APK do Jarviz usando SÓ o celular

Você não precisa de computador. A ideia: o celular só envia o código pro
GitHub, e o GitHub (nos servidores dele) compila o APK pra você de graça.
Você baixa o APK pronto depois.

Duas etapas: (1) instalar o Termux uma vez, (2) rodar alguns comandos pra
enviar esse projeto pro GitHub. Depois disso o APK sai sozinho a cada envio.

## Etapa 1 — Preparar

1. **Crie uma conta no GitHub** (se não tiver): abra github.com no navegador
   do celular e cadastre-se, é grátis.
2. **Crie um repositório novo**: no GitHub, toque em "+" > "New repository".
   Nome: `jarviz-app`. Deixe "Public". Não marque nenhuma opção de criar
   README/gitignore automaticamente. Toque em "Create repository".
3. **Crie um token de acesso** (ele substitui sua senha ao enviar código):
   vá em github.com/settings/tokens > "Generate new token" >
   "Generate new token (classic)" > marque a caixinha "repo" > gere e
   **copie o token** (começa com `ghp_...`). Guarde em algum lugar seguro
   (ex: um app de notas), você só vê ele uma vez.
4. **Instale o Termux**: pela Play Store OU (recomendado, mais atualizado)
   pelo F-Droid (f-droid.org) ou direto na página de releases do Termux no
   GitHub. É um app de terminal, gratuito.

## Etapa 2 — Enviar o projeto pro GitHub pelo Termux

Abra o Termux e digite (um comando de cada vez, apertando Enter):

```
pkg update -y && pkg install -y git unzip
termux-setup-storage
```

(Vai pedir permissão de acesso ao armazenamento — aceite.)

Agora extraia o zip que eu te mandei (ele já deve estar na pasta Downloads
do celular) e entre na pasta:

```
cd ~/storage/downloads
unzip jarviz-github-actions.zip -d jarviz-app
cd jarviz-app
```

Configure seu nome e e-mail do git (uma vez só, qualquer nome/email serve):

```
git config --global user.name "Seu Nome"
git config --global user.email "seuemail@exemplo.com"
```

Inicialize e envie pro GitHub (troque `SEU-USUARIO` pelo seu usuário do
GitHub e `SEU_TOKEN` pelo token que você copiou na Etapa 1):

```
git init
git add .
git commit -m "Jarviz god mode"
git branch -M main
git remote add origin https://SEU-USUARIO:SEU_TOKEN@github.com/SEU-USUARIO/jarviz-app.git
git push -u origin main
```

## Etapa 3 — Pegar o APK pronto

1. No navegador, abra `github.com/SEU-USUARIO/jarviz-app`.
2. Clique na aba **"Actions"** — vai ter uma execução em andamento chamada
   "Build Jarviz APK" (leva uns 3 a 6 minutos).
3. Quando terminar (ícone verde ✔️), clique nela, desça até
   **"Artifacts"** e toque em **"jarviz-apk"** — baixa um .zip.
4. Extraia esse .zip no celular (qualquer app tipo "Arquivos" ou
   "ZArchiver" abre) — dentro está o `app-debug.apk`.
5. Toque no `.apk` e instale (ative "permitir instalar de fontes
   desconhecidas" se o Android pedir).

Pronto — Jarviz rodando, sem nunca ter usado um PC.

## Se algo der errado na Etapa 3 (build falhou, ❌ vermelho)
Clique na execução que falhou, abra o log da etapa que quebrou e me manda
o texto do erro aqui no chat — eu ajusto o código ou o workflow e você só
precisa repetir `git add . && git commit -m "fix" && git push` pra
disparar um novo build.

## Alternativa mais simples (menos confiável): app AIDE
Existe um app chamado **AIDE - IDE para Android Java/C++** na Play Store
que compila projetos Android direto no celular, sem precisar de GitHub.
Ele é mais direto, mas a versão gratuita tem limitações e às vezes tem
dificuldade com projetos Gradle mais novos. Se quiser tentar: instale o
AIDE, abra-o, escolha "Importar Projeto" e aponte pra pasta extraída do
zip. Se ele reclamar de configuração do Gradle, a rota do GitHub Actions
acima é mais garantida.
