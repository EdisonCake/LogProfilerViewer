# 💻 LogProfiler View (Java Edition) — v5.1 'The Halloween Update'

Ferramenta visual para leitura e análise de arquivos `.log` e `.csv` do **Protheus**, com foco em produtividade, clareza e agilidade.
Ideal para desenvolvedores, analistas e entusiastas que lidam com logs de execução e dados exportados no dia a dia.

---

## ✨ Principais Funcionalidades

* ✅ **Leitura Abrangente**: Suporte a arquivos `console.log`, `LogProfiler`, `error.log` e arquivos de **exportaDados** (`.csv`) do Protheus.
* 🎨 **Seletor de Temas**: Interface com múltiplos temas visuais (claros e escuros) da biblioteca FlatLaf, aplicáveis em tempo real através do menu "Visual".
* 🔍 **Filtro Inteligente**: Filtro dinâmico por nome de função e termos encontrados no **callstack**.
* ⚖️ **Comparador de CSVs**: Comparação automática de arquivos `.csv`, com destaque visual das divergências entre os dados.
* 🛠 **Análise de Erros Detalhada**: Visualização dedicada para `error.log`, separando ocorrências, informações de ambiente, callstack e queries SQL.
* 📋 **Cópia Rápida de Query**: Copie a query de um `error.log` com um único clique para análise direta no banco de dados.
* 📁 **Identificação Automática**: O programa reconhece o tipo de arquivo carregado (`SHIFT+F6`, `console.log`, `error.log`, `.csv`) e ajusta a interface.
* 📈 **Tabelas Ordenáveis**: Organize os dados rapidamente clicando nos cabeçalhos das colunas.
* 🧭 **Identificação de Ambiente**: Informe o ambiente de origem (produção, homologação, etc.) dos arquivos `.csv` para facilitar a comparação.


---

## Changelog:

* Ajustada a barra de título, criando um menu abaixo com a opção de temas.
* Alterada a lógica de leitura de console.log  para abranger novos formatos de emissão.
* Corrigida a lógica de agrupamento de Threads na leitura de arquivos de console.log.
* Inicializando a aplicação com o tema "Arc Dark - Orange" porque é Halloween!
* Adicionada seção de informações sobre o programa no menu principal.
* Adicionado link atalho para portifolio online.
* Adicionados mais temas (pois nunca é demais)!!!

---

## 📦 Instruções de Uso

O programa é distribuído como um executável auto-contido para Windows.

#### 🖥️ Windows
1.  Baixe e descompacte o arquivo `.zip` da versão mais recente.
2.  Navegue até a pasta `App/Windows/LogProfiler/`.
3.  Extraia o .zip e dê dois cliques no arquivo **`LogProfiler.exe`** para iniciar o programa.

Não é necessário ter Java instalado no computador.

---

## 📝 Requisitos

#### Para Usuários (executar o programa)
* Nenhum! A versão para Windows já inclui tudo o que precisa para rodar.

#### Para Desenvolvedores (compilar o projeto)
* **JDK 14 ou superior** (para a ferramenta `jpackage`).
* **Apache Maven** configurado nas variáveis de ambiente.

---

## 🧠 Desenvolvido por

**Edison Luiz** com apoio da IA (ChatGPT), como parte de um projeto pessoal para aprendizado e contribuição à comunidade técnica.

---

## 📃 Licença

Este projeto é open-source sob a licença MIT.
Use, contribua e compartilhe sem moderação!
