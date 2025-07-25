# 💻 LogProfiler View (Java Edition) — v4.0

Ferramenta visual para leitura e análise de arquivos `.log` e `.csv` do **Protheus**, com foco em produtividade, clareza e agilidade.  
Ideal para desenvolvedores, analistas e entusiastas que lidam com logs de execução e dados exportados no dia a dia.

---

## 🧠 Desenvolvido por

**Edison Luiz** com apoio da IA (ChatGPT), como parte de um projeto pessoal para aprendizado e contribuição à comunidade técnica.

---

## ✨ Principais Funcionalidades

* ✅ Leitura de arquivos `console.log`, `LogProfiler`, `error.log` e agora também arquivos de **exportaDados** (`.csv`) do Protheus.
* 🔍 Filtro dinâmico e inteligente por nome de função e termos encontrados no **callstack**.
* 📁 Identificação automática do tipo de arquivo carregado (`SHIFT+F6`, `console.log`, `error.log`, `exportaDados.csv`).
* 🧾 Campo adicional com o nome do arquivo e ambiente (produção, homologação, teste).
* 💥 Exibição centralizada ao abrir o programa.
* 🧪 Título da janela atualizado dinamicamente com o nome do log aberto.
* 📈 Tabelas ordenáveis via clique no cabeçalho.
* 🧪 Suporte a testes manuais via clique direto ou execução por `.bat`.
* 🎨 Visual renovado e mais moderno para facilitar a análise.
* 🛠 Visualização detalhada para `error.log`, com separação de ocorrências, ambientes, callstack e queries.
* 📋 Copiar query diretamente da guia de erro para análise rápida.
* ⚖️ **Comparação automática de arquivos `.csv`**, com destaque das divergências entre os dados.
* 🧭 Identificação do ambiente de origem de cada arquivo comparado.

---

📦 Instruções de Uso

Após baixar o repositório ou os arquivos disponibilizados no GitHub, siga os passos abaixo para executar o LogProfilerView:

🔹 1. Baixe o repositório
Você pode baixar o projeto diretamente pelo botão Code > Download ZIP ou via Git:
``git clone https://github.com/EdisonCake/LogProfilerViewer.git``


🔹 2. Escolha sua plataforma

🖥️ Windows
- Navegue até a pasta ``App/Windows/``
- Dê dois cliques no arquivo ``IniciarLogProfiler.bat``

Isso abrirá a interface gráfica do LogProfiler View.

🍏 macOS
- Navegue até ``App/MacOS/LogProfilerView.app/Contents/MacOS``
- Execute o binário: ``./LogProfilerView``

Se necessário, dê permissão de execução:
``chmod +x LogProfilerView``

🐧 Linux
- Navegue até a raiz do projeto
- Dê permissão ao script:
``chmod +x start.sh``

- Execute:
``./start.sh``


📂 3. Carregando arquivos
Você pode carregar os seguintes tipos de arquivos:
- console.log, LogProfiler.log, error.log
- Arquivos .csv gerados por exportaDados do Protheus
O programa identifica automaticamente o tipo de arquivo e exibe a interface correspondente.


⚖️ 4. Comparação de arquivos .csv
- Carregue dois arquivos .csv de exportação
- Informe o ambiente de origem de cada um (produção, homologação, teste)
- Clique no botão Comparar
- O programa exibirá as divergências entre os arquivos, destacando os campos diferentes

🧾 5. Guia de Erros (error.log)
- Exibe ocorrências separadas por ambiente, função e callstack
- Se houver erro de query, você pode copiar a query diretamente para análise

✅ Dicas
- Use os botões rápidos para filtrar funções customizadas (U_) ou internas
- Clique nos cabeçalhos das tabelas para ordenar os dados
- Use o botão de limpar filtros para restaurar a visualização original

---
📁 Estrutura do Repositório:

```
LogProfilerViewer/
├── App/
│   ├── assets/
│   │   └── img/ <-- Imagens/prévias da aplicação
│   ├── MacOS/
│   │   └── LogProfilerView.app/ <-- Aplicativo para rodar no MAC apenas com um clique.
│   │       └── Contents/
│   │           ├── MacOS/
│   │           │   └── LogProfilerView
│   │           ├── Resources/
│   │           │   └── LogProfilerView.jar
│   │           └── Info.plist
│   └── Windows/
│       ├── iniciar.bat             <-- Pode iniciar tanto por aqui
│       └── LogProfilerView.jar     <-- quanto por aqui!
```
---
🔍 Detalhes:
- App/assets/img/: Contém imagens de preview da interface.
- App/MacOS/: Estrutura do aplicativo para macOS, com binários e recursos.
- App/Windows/: Scripts e JAR para execução no Windows.

---
📝 Requisitos
- Java 11 ou superior instalado
- Sistema com suporte a interface gráfica (Swing)

---
🖥️ Galeria:

⚖️ Interface simples
<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172719.png" alt="Interface simples" width="600"/><br><em>Visualização separada de threads, rotinas e filtros.</em></div>

🛠 Visualização Detalhada de error.log
<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172743.png" alt="Visualização Detalhada de error.log" width="600"/><br><em>Separação clara por ocorrência, ambiente, função e callstack.</em></div>

📋 Copiar Query para Análise
<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172746.png" alt="Query copiável" width="600"/><br><em>Erro de query? Copie com um clique para investigar diretamente no banco.</em></div>

🔍 Comparação de CSVs
<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172750.png" alt="Comparação de CSVs" width="600"/><br><em>Filtragem inteligente por nome de função e termos do callstack.</em></div>

🎨 Seleção de ambiente
<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172820.png" alt="Seleção de Ambiente" width="600"/><br><em>Selecione qual a origem do arquivo sendo analisado para melhor identificação visual.</em></div>

<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172840.png" alt="Visualização de ambiente" width="600"/><br></div>

<div align="center"><img src="LogProfilerViewer/App/assets/img/LogProfilerView_feature 2025-07-25 172849.png" alt="Botões rápidos" width="600"/><br></div>


---
📌 Observações
- O programa não coleta dados, não envia informações para a internet e funciona 100% offline.
- Foco principal é a análise de logs CALL, -- FROM, error.log e agora também arquivos de exportação de dados (.csv).
- Versão atual: v4.0

---
🔮 Próximas Features (roadmap pessoal)
- Multilinguagem (PT-BR/EN)
- Modo escuro 🌙

---
🧱 Autor
Desenvolvido por Edison Luiz (Cake)
Entre uma música no Just Dance e um surto, ele resolveu fazer essa ferramenta aqui 🕺🥝🚗

---
📃 Licença
Este projeto é open-source sob a licença MIT.
Use, contribua e compartilhe sem moderação!
