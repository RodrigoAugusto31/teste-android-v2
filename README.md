# SPTransApp

Este é um aplicativo Android que consome a API [Olho Vivo](http://www.sptrans.com.br/desenvolvedores/APIOlhoVivo/Documentacao.aspx) da SPTrans para fornecer informações em tempo real sobre o transporte público da cidade de São Paulo.

## Funcionalidades

*   **Pesquisa de Linhas**: Encontre linhas de ônibus pelo nome ou número.
*   **Visualização no Mapa**: Veja a posição dos ônibus em tempo real no mapa.
*   **Previsão de Chegada**: Consulte a previsão de chegada dos ônibus em uma determinada parada.
*   **Paradas de Ônibus**: Visualize as paradas de uma linha específica.
*   **Corredores de Ônibus**: Exibe os corredores de ônibus da cidade no mapa.
*   **Favoritos**: Salve suas linhas de ônibus favoritas para acesso rápido (funcionalidade inferida a partir dos casos de uso `GetFavoritesUseCase` e `ToggleFavoriteUseCase`).

## Arquitetura

O projeto segue os princípios da **Arquitetura Limpa (Clean Architecture)**, dividindo o código em três camadas principais:

*   **Presentation**: Camada de interface com o usuário (Activities, Fragments, ViewModels), utilizando o padrão MVVM.
*   **Domain**: Camada de regras de negócio da aplicação (Use Cases).
*   **Data**: Camada de acesso a dados, responsável por buscar informações da API da SPTrans (usando Retrofit) e do banco de dados local (usando Room).

A **Injeção de Dependência** é gerenciada pelo [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).

## Tecnologias Utilizadas

*   [Kotlin](https://kotlinlang.org/): Linguagem de programação principal.
*   [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html): Para gerenciamento de tarefas assíncronas.
*   [Android Jetpack](https://developer.android.com/jetpack):
    *   [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel): Para gerenciar dados da UI de forma consciente do ciclo de vida.
    *   [LiveData](https://developer.android.com/topic/libraries/architecture/livedata): Para notificar a UI sobre mudanças nos dados.
    *   [Navigation Component](https://developer.android.com/guide/navigation): Para gerenciar a navegação entre as telas.
    *   [Room](https://developer.android.com/training/data-storage/room): Para persistência de dados local.
*   [Retrofit](https://square.github.io/retrofit/): Para realizar chamadas de rede à API da SPTrans.
*   [Hilt](https://developer.android.com/training/dependency-injection/hilt-android): Para injeção de dependência.
*   [Google Maps SDK](https://developers.google.com/maps/documentation/android-sdk/start): Para exibição dos mapas.

## Como Compilar e Executar

1.  Clone este repositório.
2.  Abra o projeto no Android Studio.
3.  Crie um arquivo `local.properties` na raiz do projeto, caso ele não exista.
4.  Adicione as seguintes chaves a este arquivo com seus respectivos tokens de acesso:
    ```properties
    SPTRANS_TOKEN="SEU_TOKEN_DA_SPTRANS"
    MAPS_API_KEY="SUA_CHAVE_DO_GOOGLE_MAPS"
    ```
5.  Compile e execute o aplicativo em um emulador ou dispositivo Android.
