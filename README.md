# 💻 LogProfiler View (Java Edition)

Programa para a leitura de arquivos `.log` do Protheus, gerados a partir das configurações do AppServer ou direto na execução da rotina com `SHIFT + F6`.
Ideal para desenvolvedores, analistas e entusiastas que lidam com logs de execução no dia a dia e querem uma ferramenta visual rápida, prática e gratuita.

---

## 🧠 Desenvolvido por

**Edison Luiz** com apoio da IA (ChatGPT), como parte de um projeto pessoal para aprendizado e contribuição à comunidade técnica.

---

## ✨ Principais Funcionalidades

* ✅ Leitura de arquivos `console.log` e `LogProfiler` do Protheus, identificando e separando corretamente as funções e chamadas.
* 🔍 Filtro dinâmico e inteligente por nome de função e também por termos encontrados no **callstack**.
* 🧹 Botão para limpar filtros com resgate imediato da pilha original.
* 📁 Identificação automática do tipo de arquivo carregado (SHIFT+F6, console.log, error.log).
* 🎯 Botões de filtro rápido:

  * `Exibir Customizados (U_)`
  * `Exibir Funções Internas`
* 💥 Exibição centralizada ao abrir o programa.
* 🧾 Campo adicional com o nome do arquivo carregado.
* 🧪 Título da janela atualizado dinamicamente com o nome do log aberto.
* 📈 Tabelas ordenáveis via clique no cabeçalho.
* 🧪 Suporte a testes manuais via clique direto ou execução por `.bat`.

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
* Foco principal é a análise de logs `CALL` e `-- FROM`, com visual amigável e suporte a contextos complexos.
* Versão atual: `v1.1000050`

---

## 🔮 Próximas Features (roadmap pessoal)

* Exportação dos resultados filtrados para `.csv`
* Multilinguagem (PT-BR/EN)
* Leitura de arquivos `error.log` com visualização simplificada de erros e pilhas

---

## 🧱 Autor

Desenvolvido por [Edison Luiz (Cake)](https://github.com/edisoncake)
Entre uma música no Just Dance e um surto, ele resolveu fazer isso aqui 🕺🥝🚗

---

## 🗄️ Preview

Exibição customizada (colorida) para funções customizadas e codeblocks.

![Preview](https://raw.githubusercontent.com/EdisonCake/LogProfilerViewer/main/LogProfileView/assets/img/LogProfilerView_preview.png)

---

## 📃 Licença

Este projeto é open-source sob a licença [MIT](LICENSE).
Use, contribua e compartilhe sem moderação!
