# Vila de Ferro

Mod para Minecraft Java 1.20.1 com Forge 47.2.0. Adiciona o Villager Guerreiro e o Chefe Villager.

## O que vem no mod

- Villagers guerreiros neutros: só entram em combate quando provocados ou quando detectam monstros hostis.
- Guerreiros usam espada ou arco e aparecem equipados com armaduras de couro e ferro.
- Quando um guerreiro ou o chefe sofre dano causado por uma entidade, aliados da mesma vila num raio de 24 blocos revidam contra o agressor.
- O chefe invoca até três guerreiros a cada 9 segundos durante combate, respeitando um limite de seis guerreiros próximos.
- Ovos de invocação para as duas entidades na aba de ovos do Criativo.

## Compilar

Necessário JDK 17 e Gradle 8.1.1 (o wrapper já está incluso: `./gradlew build` ou `gradlew.bat build`). O .jar sai em `build/libs/viladeferro-1.0.0.jar`. Também há um workflow do GitHub Actions (`.github/workflows/build.yml`) que gera o .jar automaticamente.

## Publicação

O projeto usa licença MIT. Para publicar o mod, inclua o arquivo `LICENSE` e informe compatibilidade com Minecraft 1.20.1 e Forge 47.x. O nome de autor em `gradle.properties` está como texto genérico e deve ser alterado antes da publicação.

## IDs

- `viladeferro:guerreiro_villager`
- `viladeferro:chefe_villager`
- `/summon viladeferro:guerreiro_villager`
- `/summon viladeferro:chefe_villager`
