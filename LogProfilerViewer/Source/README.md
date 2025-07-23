# 💻 LogProfiler View (Java Edition)

Uma interface gráfica feita em Java para leitura de logs do Protheus (TOTVS), com foco em análise visual e usabilidade.  
Ideal para desenvolvedores, analistas e entusiastas que lidam com logs no dia a dia e querem uma ferramenta visual rápida, prática e gratuita.

---

## 🧠 Desenvolvido por

**Edison Luiz** com apoio da IA (ChatGPT), como parte de um projeto pessoal para aprendizado e contribuição à comunidade técnica.

---

## ✨ Principais Funcionalidades

- ✅ Leitura de arquivos `console.log` e `LogProfiler` do Protheus.
- 🔍 Filtro dinâmico e inteligente de funções por nome.
- 🧩 Exibição vinculada da pilha de chamadas (`-- FROM`) ao clicar numa função.
- 🧹 Botão para limpar filtros com resgate de pilha.
- 📁 Indicação automática do tipo de arquivo carregado.
- 🎯 Botões de filtro rápido:
  - `Exibir Customizados (U_)`
  - `Exibir Internos (coluna Fonte)`
- 🌒 Modo Dark/Light toggle com botão.
- 🔢 Máscara de ordenação formatada: `# 00000X`
- 🎨 Layout otimizado com painel superior unificado para botões e filtro.
- 📈 Tabelas ordenáveis via clique no cabeçalho.
- 🧪 Suporte a testes manuais via clique direto ou execução por `.bat`.

---

## 🗂️ Estrutura do Projeto

```
LogProfilerViewer/
├── build/                   # Diretório gerado com os .class compilados
├── src/                     # Código-fonte Java
│   └── LogProfilerView.java
├── IniciarLogProfiler.bat   # Executável de atalho para Windows
├── start.sh                 # Executável de atalho para Linux/macOS
├── manifest.txt             # Manifesto do JAR
├── README.md                # Este arquivo
```

---

## ▶️ Como Executar

### 🔵 Windows

Basta dar **dois cliques** no arquivo:

```bat
IniciarLogProfiler.bat
```

> Ele compila, inicia o programa e esconde o terminal automaticamente.

### 🟢 Linux/macOS

Execute o script bash:

```bash
chmod +x start.sh
./start.sh
```

---

## 📝 Requisitos

- Java 8 ou superior instalado (recomenda-se JDK 11+)
- Sistema com suporte a interface gráfica

---

## 📌 Observações

- O programa **não coleta dados**, **não envia informações** para a internet e funciona 100% offline.
- Foco principal é a análise de logs `CALL` e `-- FROM`, com visual amigável.
- Versão atual: `v1.1000050`

---

## 🔮 Próximas Features (roadmap pessoal)

- Exportação dos resultados filtrados para `.csv`
- Geração de gráfico com tempo das rotinas
- Histórico de arquivos carregados
- Multilinguagem (PT-BR/EN)

---

## 🧁 Autor

Desenvolvido por [Edison Luiz (Cake)](https://github.com/edisoncake)  
Entre uma música no Just Dance e outra no Rock Band, ele resolveu fazer isso aqui 🕺🎸

---

## 📷 Preview

*(Em breve aqui! Pode mandar o print que eu insiro!)*

---

## 📃 Licença

Este projeto é open-source sob a licença [MIT](LICENSE).  
Use, contribua e compartilhe!