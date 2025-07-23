# 💻 LogProfiler View (Java Edition)

Programa para a leitura de arquivos `.log` do Protheus, gerados a partir das configurações do AppServer ou direto na execução da rotina com `SHIFT + F6`.  
Ideal para desenvolvedores, analistas e entusiastas que lidam com logs de execução no dia a dia e querem uma ferramenta visual rápida, prática e gratuita.

---

## 🧠 Desenvolvido por

**Edison Luiz** com apoio da IA (ChatGPT), como parte de um projeto pessoal para aprendizado e contribuição à comunidade técnica.

---

## ✨ Principais Funcionalidades

* ✅ Leitura de arquivos `console.log`, `LogProfiler` e agora também `error.log` do Protheus, com identificação e separação correta de funções, chamadas, ocorrências, ambientes e queries utilizadas.
* 🔍 Filtro dinâmico e inteligente por nome de função e termos encontrados no **callstack**.
* 🧹 Botão para limpar filtros com resgate imediato da pilha original.
* 📁 Identificação automática do tipo de arquivo carregado (`SHIFT+F6`, `console.log`, `error.log`).
* 🎯 Botões de filtro rápido:
  * `Exibir Customizados (U_)`
  * `Exibir Funções Internas`
* 💥 Exibição centralizada ao abrir o programa.
* 🧾 Campo adicional com o nome do arquivo carregado.
* 🧪 Título da janela atualizado dinamicamente com o nome do log aberto.
* 📈 Tabelas ordenáveis via clique no cabeçalho.
* 🧪 Suporte a testes manuais via clique direto ou execução por `.bat`.
* 🎨 Visual renovado, mais moderno e agradável para facilitar a análise.
* 🛠 Visualização detalhada para `error.log`, com separação das ocorrências, ambientes, callstack e queries quando presentes.

---

## ▶️ Como Executar

### 🔵 Windows

Basta dar **dois cliques** no arquivo:

```bat
IniciarLogProfiler.bat
```

### 🔸 Linux/macOS

1. Dê permissão de execução no script:

```bash
chmod +x start.sh
./start.sh
```

---

## 📝 Requisitos

* Java **11 ou superior** instalado  
* Sistema com suporte a interface gráfica (Swing)

---

## 📌 Observações

* O programa **não coleta dados**, **não envia informações** para a internet e funciona 100% offline.  
* Foco principal é a análise de logs `CALL`, `-- FROM` e agora também `error.log`, com visual amigável e suporte a contextos complexos.  
* Versão atual: `v3.0 Alpha`

---

## 🔮 Próximas Features (roadmap pessoal)

* Exportação dos resultados filtrados para `.csv`  
* Multilinguagem (PT-BR/EN)  
* Histórico de arquivos carregados  
* Geração de gráficos com tempo das rotinas  

---

## 🧱 Autor

Desenvolvido por [Edison Luiz (Cake)](https://github.com/edisoncake)  
Entre uma música no Just Dance e um surto, ele resolveu fazer essa ferramenta aqui 🕺🥝🚗

---

## 🗄️ Preview

Exibição customizada (colorida) para funções customizadas, codeblocks e visualização detalhada de erros.

![Preview](https://raw.githubusercontent.com/EdisonCake/LogProfilerViewer/main/LogProfileView/assets/img/LogProfilerView_preview.png)

---

## 📃 Licença

Este projeto é open-source sob a licença [MIT](LICENSE).  
Use, contribua e compartilhe sem moderação!
