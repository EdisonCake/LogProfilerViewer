# 💻 LogProfiler View (Java Edition)

Programa para a leitura de arquivos .log do Proteus, gerados a partir das configurações do appserver, ou direto na execução da rotina com SHIFT + F6. 
Ideal para desenvolvedores, analistas e entusiastas que lidam com logs de execução no dia a dia e querem uma ferramenta visual rápida, prática e gratuita.

---

## 🧠 Desenvolvido por

**Edison Luiz** com apoio da IA (ChatGPT), como parte de um projeto pessoal para aprendizado e contribuição à comunidade técnica.

---

## ✨ Principais Funcionalidades

- ✅ Leitura de arquivos `console.log` e `LogProfiler` do Protheus para identificação de LogProfiler em ambos os arquivos.
- 🔍 Filtro dinâmico e inteligente de funções por nome.
- 🧹 Botão para limpar filtros com resgate de pilha.
- 📁 Indicação automática do tipo de arquivo carregado.
- 🎯 Botões de filtro rápido:
  - `Exibir Customizados (U_)`
  - `Exibir Funções Internass`
- 🌒 Modo Dark/Light toggle com botão (eu estava com tempo livre).
- 📈 Tabelas ordenáveis via clique no cabeçalho.
- 🧪 Suporte a testes manuais via clique direto ou execução por `.bat`.

---

## 🗂️ Estrutura do Projeto

```
LogProfilerView/
├── LogProfilerView.java
├── iniciar.bat                # Executável de atalho para Windows
├── start.sh                   # Executável de atalho para Linux/macOS
```

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
chmod +x iniciar.sh

---

## 📝 Requisitos

- Java 11 ou superior instalado
- Sistema com suporte a interface gráfica

---

## 📌 Observações

- O programa **não coleta dados**, **não envia informações** para a internet e funciona 100% offline.
- Foco principal é a análise de logs `CALL` e `-- FROM`, com visual amigável.
- Versão atual: `v1.1000050`

---

## 🔮 Próximas Features (roadmap pessoal)

- Exportação dos resultados filtrados para `.csv`
- Multilinguagem (PT-BR/EN)
- Leitura de ErrorLogs para simplificação de análises

---

## 🧁 Autor

Desenvolvido por [Edison Luiz (Cake)](https://github.com/edisoncake)  
Entre uma música no Just Dance e um surto, ele resolveu fazer isso aqui 🕺

---

## 📷 Preview

Exibição customizada (colorida) para funções customizadas, e codeblocks.
![image](https://github.com/user-attachments/assets/70c8087e-73a4-4b74-af95-d76d0d9a23a7)

---

## 📃 Licença

Este projeto é open-source sob a licença [MIT](LICENSE).  
Use, contribua e compartilhe!
