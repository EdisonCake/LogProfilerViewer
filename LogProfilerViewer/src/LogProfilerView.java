/* ==============================================================
Autor: Edison Luiz
Ajuda: ChatGPT
Versão: V2.0 Alpha

Descrição:
* Interface gráfica em Java para auxiliar na leitura de arquivos
de log do Protheus, como LogProfiler, Console.log contendo Log
Profiler e Error.log.

Updates:
* Quanto há rotina customizada sendo exibida, a mesma será 
destacada em azul.
* Quando há bloco de código, o mesmo é destacado em verde.
* Filtro para rotinas customizadas ("U_").
* Filtro para rotinas internas do sistema ("Internal").

GitHub: https://github.com/edisoncake
============================================================== */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class LogProfilerView extends JFrame {

    private JTable logTable, callStackTable;
    private DefaultTableModel logTableModel, callStackTableModel;
    private JTable errorTable, envTable;
    private DefaultTableModel errorTableModel, envTableModel;
    private JTextField searchLogField;
    private JLabel tipoArquivoLabel;
    private JLabel footerLabel;
    private List<Vector<String>> fullCallStackRows = new ArrayList<>();
    private boolean showingCustomOnly = false;
    private boolean showingInternalOnly = false;
    private boolean darkMode = false;
    private JButton toggleCustomButton;
    private JButton toggleInternalButton;
    private JButton darkModeButton;
    private JTabbedPane tabbedPane;
    private List<CallStackEntry> callStackEntries = new ArrayList<>();
    private Map<String, List<CallStackEntry>> shiftF6CallMap = new HashMap<>();
    private Map<String, List<CallStackEntry>> consoleLogCallMap = new HashMap<>();
    private boolean isConsoleLog = false;
    private boolean isShiftF6Log = false;

    /* 
        Classe: CallStackEntry
        Finalidade: Representar uma entrada da pilha de chamadas (call stack) de execução de funções.
        Contexto: Utilizada para rastrear de onde partiu uma chamada em logs do Protheus ou em sistemas de análise de performance, 
                  armazenando a rotina de origem, o arquivo fonte e a linha exata do código.
     */
    public class CallStackEntry {

        // Nome da rotina de onde a chamada partiu (ex: LOGPRFCLICK)
        private String calledFrom;

        // Nome do arquivo fonte onde essa rotina está localizada (ex: HDMAPAURA.PRX)
        private String sourceFile;

        // Número da linha no arquivo onde a chamada aconteceu
        private int lineNumber;

        // Construtor da classe que inicializa os três atributos
        public CallStackEntry(String calledFrom, String sourceFile, int lineNumber) {
            this.calledFrom = calledFrom;
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
        }

        // Retorna o nome da rotina de onde a chamada se originou
        public String getCalledFrom() {
            return calledFrom;
        }

        // Retorna o nome do arquivo fonte associado à rotina chamada
        public String getSourceFile() {
            return sourceFile;
        }

        // Retorna o número da linha onde a chamada foi feita
        public int getLineNumber() {
            return lineNumber;
        }
    }

    /* 
        Classe: CallStackRenderer
        Finalidade: Personalizar a renderização de células em uma JTable, aplicando estilos visuais como
                    cores e formatação HTML para destacar informações específicas da pilha de chamadas.
        Contexto: Utilizada no LogProfilerView para exibir linhas da stack trace de forma visualmente clara,
                  diferenciando chamadas personalizadas (U_), blocos de código ({||}) e outras tags especiais.
     */
    class CallStackRenderer extends JLabel implements TableCellRenderer {

        // Cores utilizadas para fundo e texto, tanto para seleção quanto exibição padrão
        private final Color selectionBg, selectionFg, defaultFg;

        // Construtor que recebe as cores e define aparência inicial do renderer
        public CallStackRenderer(Color selectionBg, Color selectionFg, Color defaultFg) {
            setOpaque(true); // Permite que o fundo da célula seja visível
            this.selectionBg = selectionBg;
            this.selectionFg = selectionFg;
            this.defaultFg = defaultFg;

            // Centraliza o conteúdo na célula, tanto vertical quanto horizontalmente
            setVerticalAlignment(SwingConstants.CENTER);
            setHorizontalAlignment(SwingConstants.CENTER);

            // Define a fonte monoespaçada (útil para exibição de logs e códigos)
            setFont(new Font("Monospaced", Font.PLAIN, 13));
        }

        // Método chamado automaticamente para renderizar cada célula da tabela
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {

            // Converte o valor da célula para string (evita null)
            String text = (value != null) ? value.toString() : "";

            // Determina a cor do texto com base no conteúdo
            Color fg;
            if (text.startsWith("U_")) {
                fg = Color.BLUE; // Rotinas customizadas
            } else if (text.contains("{||")) {
                fg = new Color(0, 200, 0); // Bloco de código (lambda ou anônimo)
            } else if (text.startsWith("#")) {
                fg = new Color(0, 200, 0); // Comentários ou metatags
            } else {
                fg = isSelected ? selectionFg : defaultFg; // Cor padrão ou de seleção
            }

            // Aplica as cores definidas
            setForeground(fg);
            setBackground(isSelected ? selectionBg : table.getBackground());

            // Usa HTML para permitir múltiplas linhas e centralizar texto
            String escapedText = escapeHtml(text).replace("\n", "<br>");
            setText("<html><div style='text-align: center;'>" + escapedText + "</div></html>");

            // Ajusta a altura da linha da tabela para caber o conteúdo renderizado
            int preferredHeight = getPreferredSize().height;
            if (table.getRowHeight(row) != preferredHeight) {
                table.setRowHeight(row, preferredHeight);
            }

            return this; // Retorna o componente configurado
        }

        // Função auxiliar para escapar caracteres especiais do HTML e evitar bugs de renderização
        private String escapeHtml(String s) {
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }

    /* 
        Classe: CustomCallRenderer
        Finalidade: Personalizar a renderização de células da tabela para permitir múltiplas linhas com quebra
                    automática de texto e aplicar cores baseadas em regras do conteúdo.
        Contexto: Usada na tabela principal do LogProfilerView para exibir chamadas (CALL) de forma legível,
                  com destaque visual para rotinas customizadas (U_) e ajuste dinâmico de altura da linha.
     */
    class CustomCallRenderer extends JTextArea implements TableCellRenderer {

        // Construtor padrão: ativa quebra de linha e opacidade
        public CustomCallRenderer() {
            setLineWrap(true);        // Permite quebrar linhas automaticamente
            setWrapStyleWord(true);   // Quebra por palavras (não no meio de uma palavra)
            setOpaque(true);          // Permite aplicar cor de fundo
        }

        // Método principal chamado ao renderizar cada célula da tabela
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            // Define o texto exibido na célula (evita null)
            setText(value != null ? value.toString() : "");

            // Obtém o valor da coluna "Nome" (índice 1 da model original)
            String nome = table.getModel().getValueAt(table.convertRowIndexToModel(row), 1).toString();

            // Define a cor do texto: azul para rotinas que começam com "U_"
            if (nome.startsWith("U_")) {
                setForeground(Color.BLUE);
            } else {
                setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            }

            // Define a cor de fundo conforme a seleção da linha
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }

            // Usa a mesma fonte da tabela
            setFont(table.getFont());

            // Ajusta a altura da linha com base no conteúdo renderizado
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
            int preferredHeight = getPreferredSize().height;
            if (table.getRowHeight(row) != preferredHeight) {
                table.setRowHeight(row, preferredHeight);
            }

            return this; // Retorna o componente renderizado
        }
    }

    /* 
        Método: LogProfilerView (Construtor)
        Finalidade: Inicializar a interface gráfica principal da aplicação LogProfilerView,
                   configurando componentes, layout, eventos e comportamento visual.
        Contexto: Executado na criação da janela principal do programa, configurando botões,
                  tabelas, filtros, modos visuais (claro/escuro) e interatividade básica.
     */
    public LogProfilerView() {
        super("LogProfiler View (Java Edition)"); // Define o título da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Fecha app ao fechar janela
        setSize(1000, 750);                      // Define tamanho da janela
        setLayout(new BorderLayout());           // Usa BorderLayout para organizar áreas

        // Criação dos botões e labels na barra superior
        JButton openFileButton = new JButton("Abrir Arquivo Log");  // Botão para abrir arquivo
        tipoArquivoLabel = new JLabel("Tipo: Nenhum arquivo carregado"); // Label que mostra tipo de arquivo carregado
        tipoArquivoLabel.setForeground(Color.BLUE); // Cor azul para o label de tipo

        darkModeButton = new JButton("Modo Dark"); // Botão para alternar tema dark/light

        // Painel superior com FlowLayout à esquerda, onde ficam os botões e label
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(openFileButton);
        topBar.add(tipoArquivoLabel);
        topBar.add(darkModeButton);

        // Painel de filtros, com campo de texto e botões de controle
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLogField = new JTextField(30);      // Campo para filtro de texto no log
        JButton clearButton = new JButton("Limpar Filtros"); // Botão para limpar filtros
        toggleCustomButton = new JButton("Exibir Customizados"); // Botão para filtrar customizados
        toggleInternalButton = new JButton("Exibir Internos");   // Botão para filtrar internos

        // Adiciona os componentes ao painel de filtros
        filterPanel.add(new JLabel("Filtro Log: "));
        filterPanel.add(searchLogField);
        filterPanel.add(clearButton);
        filterPanel.add(toggleCustomButton);
        filterPanel.add(toggleInternalButton);
        filterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Limita altura do painel filtro

        // Painel header que vai juntar topBar + filterPanel verticalmente (BoxLayout Y_AXIS)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(topBar);
        headerPanel.add(filterPanel);

        // Adiciona o headerPanel na parte de cima da janela (NORTH do BorderLayout)
        add(headerPanel, BorderLayout.NORTH);

        // Modelos das tabelas principais
        logTableModel = new DefaultTableModel(new String[]{"# Ordem", "Nome", "Fonte"}, 0);
        callStackTableModel = new DefaultTableModel(new String[]{"Chamado de", "Fonte", "Linha"}, 0);

        // Cria as tabelas baseadas nos modelos
        logTable = new JTable(logTableModel);
        callStackTable = new JTable(callStackTableModel);

        logTable.setAutoCreateRowSorter(true); // Permite ordenar as colunas da tabela
        // Usa renderer customizado para coluna "Nome" (índice 1)
        logTable.getColumnModel().getColumn(1).setCellRenderer(new CustomCallRenderer());

        // Define larguras preferenciais das colunas da tabela de logs
        logTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        // Impede reordenar e redimensionar colunas manualmente
        logTable.getTableHeader().setReorderingAllowed(false);
        logTable.getTableHeader().setResizingAllowed(false);

        // Listener para quando uma linha da tabela logTable for selecionada
        logTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Evita evento duplo
                int selectedRow = logTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < logTableModel.getRowCount()) {
                    // Pega o nome da função da linha selecionada
                    String funcName = logTableModel.getValueAt(selectedRow, 1).toString();
                    funcName = funcName.toUpperCase().trim();
                    System.out.println("DEBUG: Linha selecionada - funcName: " + funcName);

                    if (isShiftF6Log) {
                        // Debug e atualização da pilha de chamadas para logs Shift+F6
                        System.out.println("DEBUG: Entradas no shiftF6CallMap: "
                                + (shiftF6CallMap.containsKey(funcName) ? shiftF6CallMap.get(funcName).size() : "nenhuma"));
                        updateCallStackShiftF6(funcName, shiftF6CallMap);
                    } else if (isConsoleLog) {
                        // Debug e atualização da pilha para logs Console
                        System.out.println("DEBUG: Entradas no consoleLogCallMap: "
                                + (consoleLogCallMap.containsKey(funcName) ? consoleLogCallMap.get(funcName).size() : "nenhuma"));
                        updateCallStack(funcName);
                    } else {
                        System.out.println("DEBUG: Tipo de arquivo desconhecido para atualizar callStack.");
                    }
                }
            }
        });

        // Tabela e modelo para erros
        String[] colunasErro = {"Mensagem", "Rotina", "Fonte", "Linha", "Data/Hora (Versão)"};
        errorTableModel = new DefaultTableModel(colunasErro, 0);
        errorTable = new JTable(errorTableModel);

        // Usa renderer customizado na coluna "Rotina" e para todas as células da tabela de erro
        errorTable.getColumnModel().getColumn(1).setCellRenderer(new CustomCallRenderer());
        errorTable.setDefaultRenderer(Object.class, new CustomCallRenderer());

        // Tabela para mostrar informações do ambiente do erro
        envTableModel = new DefaultTableModel(new String[]{"Campo", "Valor"}, 0);
        envTable = new JTable(envTableModel);

        // Painel que junta tabela de erro e ambiente na vertical
        JPanel errorPanel = new JPanel();
        errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));
        errorPanel.add(new JScrollPane(errorTable));
        errorPanel.add(new JScrollPane(envTable));

        // Painel que junta tabela de logs e pilha de chamadas na vertical
        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
        logPanel.add(new JScrollPane(logTable));
        logPanel.add(new JScrollPane(callStackTable));

        // Cria abas para alternar entre Logs e Error Logs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Logs", logPanel);
        tabbedPane.addTab("Error Logs (testes)", errorPanel);

        // Adiciona as abas ao centro da janela
        add(tabbedPane, BorderLayout.CENTER);

        // Label rodapé com autor, versão e texto estilizado
        footerLabel = new JLabel("© Edison Luiz - v2.0 (ALPHA)   ||   (⌐■_■) Você só tinha que seguir o trem, CJ...", JLabel.CENTER);
        footerLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(footerLabel, BorderLayout.SOUTH);

        // Define ação do botão abrir arquivo
        openFileButton.addActionListener(this::handleFileOpen);

        // Listener para atualização do filtro conforme digitação no campo searchLogField
        searchLogField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterLog();
            }

            public void removeUpdate(DocumentEvent e) {
                filterLog();
            }

            public void changedUpdate(DocumentEvent e) {
                filterLog();
            }
        });

        // Botão limpar filtros: limpa texto e reset dos toggles
        clearButton.addActionListener(e -> {
            searchLogField.setText("");
            showingCustomOnly = false;
            toggleCustomButton.setText("Exibir Customizados");
            showingInternalOnly = false;
            toggleInternalButton.setText("Exibir Internos");
            filterLog();
        });

        // Botão alterna filtro customizados
        toggleCustomButton.addActionListener(e -> {
            showingCustomOnly = !showingCustomOnly;
            if (showingCustomOnly) {
                showingInternalOnly = false;
                toggleInternalButton.setText("Exibir Internos");
            }
            toggleCustomButton.setText(showingCustomOnly ? "Resetar" : "Exibir Customizados");
            filterLog();
        });

        // Botão alterna filtro internos
        toggleInternalButton.addActionListener(e -> {
            showingInternalOnly = !showingInternalOnly;
            if (showingInternalOnly) {
                showingCustomOnly = false;
                toggleCustomButton.setText("Exibir Customizados");
            }
            toggleInternalButton.setText(showingInternalOnly ? "Resetar" : "Exibir Internos");
            filterLog();
        });

        // Botão para alternar tema dark/light
        darkModeButton.addActionListener(e -> toggleDarkMode());

        // Aplica tema inicial (claro ou escuro)
        applyTheme();
    }

    /* 
        Método: readConsoleLogFile
        Finalidade: Ler e processar um arquivo do tipo console.log, extraindo trechos do APP PROFILER,
                   identificando blocos de chamadas (CALL) e suas origens (-- FROM), e atualizando as tabelas da UI.
        Contexto: Chamado ao abrir um arquivo console.log no LogProfilerView, para analisar logs de desempenho e pilha de chamadas.
     */
    private void readConsoleLogFile(File file) throws IOException {
        List<String> lines = new ArrayList<>();

        // Abre o arquivo com encoding ISO_8859_1 e lê todas as linhas para uma lista
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // Limpa tabelas e dados anteriores para novo carregamento
        logTableModel.setRowCount(0);
        callStackTableModel.setRowCount(0);
        fullCallStackRows.clear();
        callStackEntries.clear();
        tipoArquivoLabel.setText("✔️ Console.log detectado");

        // Procura de trás pra frente o índice onde começa o trecho do APP PROFILER no log
        int lastProfilerStart = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).contains("--- BEGIN APP PROFILER")) {
                lastProfilerStart = i;
                break;
            }
        }

        // Se não achar o trecho, mostra erro e sai do método
        if (lastProfilerStart == -1) {
            JOptionPane.showMessageDialog(null,
                    "Nenhum trecho de APP PROFILER encontrado no console.log",
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Pega o trecho do log que vai do início do profiler até o fim do arquivo
        List<String> trechoFinal = lines.subList(lastProfilerStart, lines.size());
        int ordem = 1; // Contador para numerar os blocos CALL

        List<String> callBlockLines = new ArrayList<>();

        // Percorre o trechoFinal para agrupar linhas de blocos CALL e linhas -- FROM relacionadas
        for (int i = 0; i < trechoFinal.size(); i++) {
            String linha = trechoFinal.get(i).trim();
            linha = cleanLine(linha); // Remove espaços extras e caracteres indesejados

            if (linha.startsWith("CALL")) {
                // Se já tem um bloco em andamento, processa antes de iniciar novo bloco CALL
                if (!callBlockLines.isEmpty()) {
                    processCallBlock(callBlockLines, ordem++, true, consoleLogCallMap);
                    callBlockLines.clear();
                }
                callBlockLines.add(linha); // Inicia novo bloco CALL
            } else if (linha.startsWith("-- FROM")) {
                // Linhas -- FROM são adicionadas ao bloco CALL atual
                if (!callBlockLines.isEmpty()) {
                    callBlockLines.add(linha);
                }
            } else {
                // Se encontrar linha que não seja CALL nem -- FROM e tiver bloco acumulado, processa ele
                if (!callBlockLines.isEmpty()) {
                    processCallBlock(callBlockLines, ordem++, true, consoleLogCallMap);
                    callBlockLines.clear();
                }
            }
        }

        // Processa bloco final se houver
        if (!callBlockLines.isEmpty()) {
            processCallBlock(callBlockLines, ordem++, true, consoleLogCallMap);
            callBlockLines.clear();
        }

        // Atualiza as views da interface com os dados carregados
        showLogProfilerViews();

        // Seleciona e atualiza a pilha de chamadas da primeira função, se existir
        if (logTableModel.getRowCount() > 0) {
            String primeiraFunc = logTableModel.getValueAt(0, 1).toString();
            updateCallStack(primeiraFunc);
        }

        // Seleciona a aba principal de logs para o usuário visualizar
        tabbedPane.setSelectedIndex(0);
    }

    /* 
        Método: filterLog
        Finalidade: Aplicar filtros dinâmicos na tabela de logs (logTable) com base no texto digitado e
                   nas opções de exibição (customizados e internos), atualizando a exibição dos dados.
        Contexto: Usado toda vez que o usuário digita no campo de busca ou alterna os botões de filtro,
                  para facilitar encontrar funções específicas no log.
     */
    private void filterLog() {
        // Pega o sorter da tabela para aplicar filtros
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) logTable.getRowSorter();

        // Pega o texto do campo de busca e remove espaços extras
        String text = searchLogField.getText().trim();

        // Lista que vai armazenar os filtros ativos
        java.util.List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

        // Se o texto não está vazio, cria filtro regex ignorando case para coluna "Nome" (índice 1)
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1));
        }

        // Se só mostrar customizados, adiciona filtro pra linhas cujo "Nome" começa com "U_"
        if (showingCustomOnly) {
            filters.add(RowFilter.regexFilter("^U_", 1));
        }

        // Se só mostrar internos, adiciona filtro para coluna "Fonte" (índice 2) contendo "internal" ignorando case
        if (showingInternalOnly) {
            filters.add(RowFilter.regexFilter("(?i)internal", 2));
        }

        // Aplica os filtros combinados (AND) ou limpa se não houver filtro
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
            callStackTableModel.setRowCount(0); // Limpa a pilha quando não filtra nada
        } else if (filters.size() == 1) {
            sorter.setRowFilter(filters.get(0)); // Aplica único filtro
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters)); // Aplica todos em AND
        }
    }

    /* 
        Método: handleFileOpen
        Finalidade: Abrir diálogo para o usuário selecionar um arquivo de log, identificar o tipo do arquivo,
                   e carregar o conteúdo corretamente na interface.
        Contexto: Chamado quando o botão "Abrir Arquivo Log" é clicado, iniciando o processo de leitura
                  e análise do arquivo selecionado pelo usuário.
     */
    private void handleFileOpen(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();       // Cria janela para selecionar arquivo
        int result = fileChooser.showOpenDialog(this);       // Exibe diálogo modal e captura resultado

        if (result == JFileChooser.APPROVE_OPTION) {         // Se o usuário selecionou um arquivo
            File selectedFile = fileChooser.getSelectedFile();

            try {
                List<String> linhas = new ArrayList<>();

                // Lê todas as linhas do arquivo com encoding ISO_8859_1
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(selectedFile), StandardCharsets.ISO_8859_1))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        linhas.add(linha);
                    }
                }

                // Variáveis para detectar o tipo de log
                boolean foundShiftF6 = false;
                boolean foundConsoleLog = false;
                boolean foundError = false;

                boolean hasBegin = false;
                boolean hasMemory = false;
                boolean hasEnd = false;

                // Percorre todas as linhas para buscar pistas do tipo do arquivo
                for (String linha : linhas) {
                    if (linha.trim().isEmpty()) {      // Ignora linhas em branco
                        continue;
                    }

                    // Detecta se contém erro de thread (error.log)
                    if (linha.toLowerCase().contains("thread error")) {
                        foundError = true;
                    }

                    // Detecta logs gerados por SHIFT+F6 (Request Profiler)
                    if (linha.contains("/* ========================================")) {
                        foundShiftF6 = true;
                    }

                    if (foundShiftF6 && linha.contains("Request Profiler Log")) {
                        foundShiftF6 = true;          // Confirmando SHIFT+F6
                    }

                    // Detecta logs do tipo console.log (APP PROFILER)
                    if (linha.contains("--- BEGIN APP PROFILER ( THREAD [")) {
                        hasBegin = true;
                    }
                    if (linha.contains("--- MEMORY LOG PROFILER ---")) {
                        hasMemory = true;
                    }
                    if (linha.contains("--- END APP PROFILER ---")) {
                        hasEnd = true;
                    }
                }

                // Só confirma console.log se tem início, memória e fim
                foundConsoleLog = hasBegin && hasMemory && hasEnd;

                // Reseta flags de tipo de arquivo antes de decidir
                isShiftF6Log = false;
                isConsoleLog = false;

                // Com base na detecção, chama o método de leitura adequado e atualiza label
                if (foundConsoleLog) {
                    isConsoleLog = true;
                    isShiftF6Log = false;
                    tipoArquivoLabel.setText("Tipo: LogProfiler (console.log)");
                    readConsoleLogFile(selectedFile);
                    tabbedPane.setSelectedIndex(0);

                } else if (foundShiftF6) {
                    isShiftF6Log = true;
                    isConsoleLog = false;
                    tipoArquivoLabel.setText("Tipo: LogProfiler (SHIFT+F6)");
                    readLogFile(selectedFile);
                    tabbedPane.setSelectedIndex(0);

                } else if (foundError) {
                    isConsoleLog = false;
                    isShiftF6Log = false;
                    tipoArquivoLabel.setText("Tipo: Error.log");
                    readErrorLogFile(selectedFile);
                    tabbedPane.setSelectedIndex(1);

                } else {
                    // Se não reconhece tipo, avisa o usuário
                    JOptionPane.showMessageDialog(this, "Tipo de log não reconhecido.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }

            } catch (IOException ex) {
                // Em caso de erro na leitura do arquivo, exibe mensagem e imprime stack trace
                JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /* 
        Método: cleanLine
        Finalidade: Remover prefixos de timestamp, thread e outros detalhes do início da linha,
                   extraindo apenas a parte essencial da linha de log que representa uma chamada "CALL" 
                   ou uma linha de origem "-- FROM", deixando a string "limpa" para processamento.
        Contexto: Utilizado para tratar linhas de log console.log, onde as linhas podem vir com timestamps e threads 
                  no começo, mas só interessa o conteúdo da chamada ou origem para análise e parsing.
     */
    private String cleanLine(String line) {
        // Expressão regular para casar prefixo com timestamp + thread e capturar a parte CALL(...)
        Pattern prefixPattern = Pattern.compile(
                "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}-\\d{2}:\\d{2}\\s+\\d+\\|\\[Thread\\s+\\d+\\]\\s+(CALL\\s+[\\w:.]+\\s*\\([^\\)]+\\))"
        );
        Matcher matcher = prefixPattern.matcher(line);
        if (matcher.find()) {
            // Retorna só o trecho "CALL função(arquivo)" limpo, sem timestamp/thread
            return matcher.group(1);
        }

        // Expressão regular para casar prefixo timestamp + thread e capturar linha "-- FROM ..."
        Pattern fromPattern = Pattern.compile(
                "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}-\\d{2}:\\d{2}\\s+\\d+\\|\\[Thread\\s+\\d+\\]\\s+(-- FROM\\s+[\\w:.#]+\\s*\\([^\\)]+\\)\\s*(?:LN\\s+\\d+)?)"
        );
        matcher = fromPattern.matcher(line);
        if (matcher.find()) {
            // Retorna só o trecho "-- FROM rotina(arquivo) [LN linha]" limpo
            return matcher.group(1);
        }

        // Se não casar com nenhum padrão, retorna a linha original só com espaços removidos
        return line.trim();
    }

    /* 
        Método: readLogFile
        Finalidade: Ler e processar arquivos gerados pelo LogProfiler via SHIFT+F6, 
                   extraindo blocos de chamadas (CALL) e suas origens (-- FROM), 
                   e atualizar as tabelas da interface com os dados extraídos.
        Contexto: Usado para abrir logs do tipo SHIFT+F6 no LogProfilerView, que tem formato específico com blocos CALL e FROM.
     */
    private void readLogFile(File file) {
        // Limpa todas as tabelas e estruturas internas para carregar um arquivo novo
        logTableModel.setRowCount(0);
        callStackTableModel.setRowCount(0);
        errorTableModel.setRowCount(0);
        envTableModel.setRowCount(0);
        fullCallStackRows.clear();
        callStackEntries.clear();
        shiftF6CallMap.clear(); // limpa mapa global de chamadas

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            List<String> allLinesList = new ArrayList<>();

            // Lê o arquivo inteiro linha por linha, guardando cada linha original numa lista
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
                allLinesList.add(line);
            }

            String all = content.toString();
            String[] allLines = allLinesList.toArray(new String[0]);

            int beginIndex = -1;

            // Procura, de trás pra frente, o início do bloco SHIFT+F6 (linha que começa com /* ========================================)
            for (int i = allLines.length - 1; i >= 0; i--) {
                String raw = allLines[i];
                String clean = raw.trim();

                if (clean.startsWith("/* ========================================")) {
                    beginIndex = all.indexOf(raw); // pega o índice real no texto completo
                    break;
                }
            }

            // Se não encontrou o início do bloco, avisa que o arquivo não é válido para SHIFT+F6
            if (beginIndex == -1) {
                tipoArquivoLabel.setText("❌ Arquivo não é um LogProfiler válido (SHIFT+F6 não encontrado).");
                JOptionPane.showMessageDialog(this,
                        "Arquivo selecionado não contém um bloco válido de LogProfiler gerado via SHIFT+F6.\n"
                        + "Por favor, selecione um arquivo compatível.",
                        "Arquivo Inválido",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Define o fim do trecho como o final do arquivo
            int endIndex = all.length();

            // Atualiza label para informar tipo do arquivo detectado
            tipoArquivoLabel.setText("✔️ LogProfiler (SHIFT+F6)");

            // Extrai trecho do log a partir do índice encontrado até o fim
            String trechoLog = all.substring(beginIndex, endIndex);
            String[] rawLines = trechoLog.split("\n");

            Set<String> chamadasRegistradas = new HashSet<>();
            int order = 1; // contador para numerar os blocos
            List<String> currentBlock = new ArrayList<>();

            // Percorre as linhas extraídas para identificar blocos CALL e linhas FROM relacionadas
            for (String rawLine : rawLines) {
                String l = rawLine.trim();

                // Se a linha estiver vazia e tiver um bloco acumulado, processa ele
                if (l.isEmpty()) {
                    if (!currentBlock.isEmpty()) {
                        String key = currentBlock.get(0).trim();
                        if (key.startsWith("CALL") && !chamadasRegistradas.contains(key)) {
                            processCallBlock(currentBlock, order++, false, shiftF6CallMap); // false = não é console.log
                            chamadasRegistradas.add(key);
                        }
                        currentBlock.clear();
                    }
                    continue;
                }

                // Se linha começa com CALL e já tem bloco acumulado, processa o bloco anterior antes de iniciar novo
                if (l.startsWith("CALL")) {
                    if (!currentBlock.isEmpty()) {
                        String key = currentBlock.get(0).trim();
                        if (key.startsWith("CALL") && !chamadasRegistradas.contains(key)) {
                            processCallBlock(currentBlock, order++, false, shiftF6CallMap);
                            chamadasRegistradas.add(key);
                        }
                        currentBlock.clear();
                    }
                    currentBlock.add(l);
                } else if (l.startsWith("-- FROM")) {
                    // Linhas -- FROM são adicionadas ao bloco atual
                    currentBlock.add(l);
                }
            }

            // Processa último bloco, se ainda tiver um não processado
            if (!currentBlock.isEmpty() && currentBlock.get(0).startsWith("CALL")) {
                processCallBlock(currentBlock, order++, false, shiftF6CallMap);
            }

            // Atualiza as tabelas e views da interface com os dados carregados
            showLogProfilerViews();

            // Seleciona automaticamente a primeira função na tabela e atualiza a pilha de chamadas
            if (logTableModel.getRowCount() > 0) {
                logTable.setRowSelectionInterval(0, 0);
                String primeiraFunc = logTableModel.getValueAt(0, 1).toString();
                updateCallStack(primeiraFunc);
            }

            // Abre a aba principal de logs para o usuário
            tabbedPane.setSelectedIndex(0);

        } catch (IOException ex) {
            // Se der erro na leitura, avisa o usuário e imprime erro no console
            JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // Atualiza a callStack para logs Shift+F6 e console.log
    // Se for console.log, mostra tudo (sem filtro), senão filtra pelo mapa shiftF6CallMap
    /* 
        Método: updateCallStack
        Finalidade: Atualizar a tabela da pilha de chamadas (callStackTable) exibindo as rotinas que chamaram
                   a função selecionada, com origem, arquivo fonte e linha do código.
        Contexto: Usado quando o usuário seleciona uma função na tabela principal de logs para mostrar 
                  a pilha de chamadas associada, com dados vindos dos mapas carregados do console.log ou SHIFT+F6.
     */
    private void updateCallStack(String funcName) {
        if (funcName == null || funcName.trim().isEmpty()) {
            // System.out.println("DEBUG: funcName é nulo ou vazio. Nada será feito.");
            return; // Nada a fazer se nome vazio
        }

        funcName = funcName.toUpperCase().trim(); // Normaliza nome para buscar no mapa
        String key = funcName;
        callStackTableModel.setRowCount(0); // Limpa tabela antes de atualizar

        // Verifica se algum tipo de log válido está ativo
        boolean isLogValido = isConsoleLog || isShiftF6Log;
        // Escolhe o mapa certo conforme o tipo de log ativo
        Map<String, List<CallStackEntry>> callMap = isConsoleLog ? consoleLogCallMap : shiftF6CallMap;

        // System.out.println("DEBUG: Atualizando callStack para " + logTipo + " com funcName: '" + key + "'");
        // System.out.println("DEBUG: chaves no map: " + callMap.keySet());
        if (!isLogValido) {
            // System.out.println("DEBUG: Nenhum tipo de log ativo. callMap não será utilizado.");
            return; // Sem log válido, nada para mostrar
        }

        // Busca as entradas de pilha de chamadas para a função selecionada
        List<CallStackEntry> entries = callMap.get(key);

        if (entries == null || entries.isEmpty()) {
            // System.out.println("DEBUG: Func '" + key + "' com 0 entradas FROM salvas no map de " + logTipo);
            return; // Nenhuma entrada para mostrar
        }

        // System.out.println("DEBUG: Func '" + key + "' com " + entries.size() + " entradas FROM salvas no map de " + logTipo);
        // Adiciona cada entrada na tabela da pilha, mostrando chamada, fonte e linha
        for (CallStackEntry entry : entries) {
            callStackTableModel.addRow(new Object[]{
                entry.getCalledFrom(),
                entry.getSourceFile(),
                entry.getLineNumber() > 0 ? entry.getLineNumber() : "N/A"
            });
        }
    }

    /* 
        Método: updateCallStackShiftF6
        Finalidade: Atualizar a tabela da pilha de chamadas (callStackTable) para logs do tipo SHIFT+F6,
                   mostrando as rotinas que chamaram a função selecionada.
        Contexto: Usado ao selecionar uma função no log SHIFT+F6, para exibir as origens das chamadas
                  baseadas nos dados carregados nesse tipo específico de log.
     */
    private void updateCallStackShiftF6(String selectedFunc, Map<String, List<CallStackEntry>> callMap) {
        callStackTableModel.setRowCount(0); // Limpa a tabela antes de popular

        if (selectedFunc == null || selectedFunc.isEmpty()) {
            // System.out.println("DEBUG ShiftF6: selectedFunc é nulo ou vazio. Saindo...");
            return; // Sem função selecionada, não faz nada
        }

        selectedFunc = selectedFunc.toUpperCase().trim(); // Normaliza o nome da função
        // System.out.println("DEBUG ShiftF6: selectedFunc (normalizado) = " + selectedFunc);

        List<CallStackEntry> entries = callMap.get(selectedFunc); // Busca entradas no mapa
        // System.out.println("DEBUG ShiftF6: entries = " + (entries == null ? "null" : entries.size()));

        if (entries != null) {
            // Adiciona cada entrada na tabela, mostrando origem, fonte e linha
            for (CallStackEntry entry : entries) {
                System.out.println("DEBUG ShiftF6 Entry: calledFrom=" + entry.getCalledFrom()
                        + ", sourceFile=" + entry.getSourceFile()
                        + ", lineNumber=" + entry.getLineNumber());

                callStackTableModel.addRow(new Object[]{
                    entry.getCalledFrom(),
                    entry.getSourceFile(),
                    entry.getLineNumber() > 0 ? entry.getLineNumber() : "N/A"
                });
            }
        }
    }

    /* 
        Método: processCallBlock
        Finalidade: Processar um bloco de linhas de log que representam uma chamada "CALL" e suas origens "-- FROM",
                   extrair os dados relevantes e armazenar nas estruturas para exibição na interface.
        Contexto: Usado durante a leitura dos logs para interpretar os blocos de chamadas e construir a pilha de chamadas
                  tanto para logs console.log quanto SHIFT+F6, alimentando os mapas e tabelas do programa.
     */
    private void processCallBlock(List<String> blockLines, int order, boolean isConsole, Map<String, List<CallStackEntry>> mapToUse) {
        if (blockLines.isEmpty()) {
            return; // Se o bloco não tem linhas, sai fora
        }

        // Pega a primeira linha do bloco, que deve ser a linha CALL
        String callLine = blockLines.get(0).trim();

        // Regex para capturar nome da função e arquivo fonte na linha CALL
        Pattern callPattern = Pattern.compile("CALL\\s+([^\\s]+)\\s*\\(([^)]+)\\)");
        Matcher callMatcher = callPattern.matcher(callLine);

        // Se não casar com o padrão esperado, ignora o bloco
        if (!callMatcher.find()) {
            // System.out.println("⚠️ Linha CALL mal formatada, ignora: " + callLine);
            return;
        }

        // Extrai nome da função e arquivo fonte
        String funcName = callMatcher.group(1).trim();
        String sourceFile = callMatcher.group(2).trim();

        // Adiciona a função na tabela principal de logs com ordem formatada e nome em maiúsculas
        logTableModel.addRow(new Object[]{
            String.format("# %06d", order),
            funcName.toUpperCase(),
            sourceFile
        });

        // Lista que vai armazenar as entradas da pilha de chamadas desse bloco
        List<CallStackEntry> callStackList = new ArrayList<>();

        // Percorre as linhas seguintes do bloco, que devem ser linhas "-- FROM"
        for (int i = 1; i < blockLines.size(); i++) {
            String fromLine = blockLines.get(i).trim();

            // Regex para capturar chamada FROM com rotina, arquivo fonte e número da linha (opcional)
            Pattern fromPattern = Pattern.compile("-- FROM\\s+([^\\s]+)\\s*\\(([^)]+)\\)(?:\\s*\\((\\d+)\\)|\\s*LN\\s+(\\d+))?");
            Matcher fromMatcher = fromPattern.matcher(fromLine);

            // Valores padrão para caso não encontre
            String calledFrom = "-";
            String fromFile = "-";
            int fromLineNumber = -1;

            if (fromMatcher.find()) {
                calledFrom = fromMatcher.group(1).trim();
                fromFile = fromMatcher.group(2).trim();

                // Pode ter grupo 3 ou 4 para linha, pega o que não for nulo
                String lineGroup = fromMatcher.group(3) != null ? fromMatcher.group(3) : fromMatcher.group(4);
                if (lineGroup != null) {
                    try {
                        fromLineNumber = Integer.parseInt(lineGroup.trim());
                    } catch (NumberFormatException ignored) {
                        // Se falhar ao converter, deixa como -1
                    }
                }
            }

            // Cria objeto CallStackEntry com dados extraídos
            CallStackEntry entry = new CallStackEntry(calledFrom, fromFile, fromLineNumber);
            callStackList.add(entry);

            // Adiciona os dados para a tabela da pilha de chamadas completa
            fullCallStackRows.add(new Vector<>(Arrays.asList(calledFrom, fromFile, String.valueOf(fromLineNumber))));
        }

        // Normaliza a chave para o mapa (nome da função em maiúsculas)
        String key = funcName.toUpperCase().trim();

        // Salva a lista de entradas no mapa correspondente
        mapToUse.put(key, callStackList);
    }

    /* 
        Método: readErrorLogFile
        Finalidade: Ler e processar arquivos de log de erros (error.log), detectando o tipo do log,
                   extraindo erros, pilha de chamadas e ambiente, e atualizando as tabelas da interface.
        Contexto: Usado para abrir arquivos error.log do Protheus, podendo também identificar e redirecionar
                  para leitura de console.log, exibindo as informações de erro e contexto no LogProfilerView.
     */
    private void readErrorLogFile(File file) throws IOException {
        // Lê todas as linhas do arquivo numa lista
        List<String> lines = Files.readAllLines(file.toPath());

        // Verifica se é um console.log pelo cabeçalho típico
        boolean isConsoleLog = lines.stream().anyMatch(l -> l.contains("--- BEGIN APP PROFILER"));
        if (isConsoleLog) {
            readConsoleLogFile(file);  // Se for console.log, chama método específico para ele
            return;
        }

        // Limpa tabelas e estruturas para abrir arquivo novo
        errorTableModel.setRowCount(0);
        logTableModel.setRowCount(0);
        envTableModel.setRowCount(0);
        fullCallStackRows.clear();

        tipoArquivoLabel.setText("✔️ Error.log detectado");

        // Variáveis para guardar dados do erro atual e estado de leitura
        String currentError = "", rotina = "", fonte = "", linha = "", dataHora = "";
        boolean foundError = false, lendoBlocoAmbiente = false;
        List<String> ambienteLinhas = new ArrayList<>();

        // Percorre todas as linhas do arquivo
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Lê bloco de ambiente TOTVS, agrupando linhas até acabar
            if (lendoBlocoAmbiente) {
                if (!line.startsWith("[TOTVS ")) {
                    lendoBlocoAmbiente = false;
                    for (String ambienteLine : ambienteLinhas) {
                        if (ambienteLine.contains(":")) {
                            String[] parts = ambienteLine.split(":", 2);
                            envTableModel.addRow(new Object[]{parts[0].trim(), parts[1].trim()});
                        } else {
                            envTableModel.addRow(new Object[]{ambienteLine, ""});
                        }
                    }
                    ambienteLinhas.clear();
                    break; // Sai do loop, pois terminou bloco ambiente
                } else {
                    ambienteLinhas.add(line);
                    continue;
                }
            } else if (line.startsWith("[TOTVS Environment:")) {
                lendoBlocoAmbiente = true;
                ambienteLinhas.add(line);
                continue;
            }

            // Ignora linhas de "thread error"
            if (line.toLowerCase().startsWith("thread error")) {
                continue;
            }

            // Detecta linhas que contenham palavras indicativas de erro (variable, object, error, invalid, syntax)
            if (line.matches("(?i).*(variable|object|error|invalid|syntax).*")) {
                // Tenta extrair dados do erro com regex específico
                Pattern pattern = Pattern.compile("(.*) on (.*?)\\((.*?)\\) (\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}) line ?: ?(\\d+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    currentError = matcher.group(1).trim();
                    rotina = matcher.group(2).trim();
                    fonte = matcher.group(3).trim();
                    dataHora = matcher.group(4).trim();
                    linha = matcher.group(5).trim();
                } else {
                    // Caso não encontre padrão, pega só a palavra-chave do erro e deixa outros campos vazios
                    currentError = line.replaceAll(".*?(variable|object|error|invalid|syntax)", "$1").trim();
                    rotina = fonte = linha = dataHora = "";
                }

                // Adiciona o erro na tabela de erros para visualização
                errorTableModel.addRow(new Object[]{
                    capitalizeFirstLetter(currentError), rotina, fonte, linha, dataHora
                });

                foundError = true;
            }

            // Captura linhas de pilha de chamadas no formato "Called from ..."
            if (line.startsWith("Called from ")) {
                Vector<String> row = parseFromLine(line.replace("Called from ", "").trim());
                fullCallStackRows.add(row);

                // Adiciona na tabela principal de logs as chamadas encontradas
                logTableModel.addRow(new Object[]{
                    String.format("# %06d", logTableModel.getRowCount() + 1),
                    row.get(0), // rotina
                    row.get(1) // fonte
                });
            }
        }

        // Mostra as views apropriadas dependendo do que foi encontrado
        if (!fullCallStackRows.isEmpty()) {
            showLogProfilerViews();
            tabbedPane.setSelectedIndex(0); // Abre aba de logs
        } else if (foundError) {
            showErrorLogViews();
            tabbedPane.setSelectedIndex(1); // Abre aba de erros
        } else {
            // Se nada foi encontrado, avisa o usuário
            JOptionPane.showMessageDialog(null, "Nenhum erro encontrado no arquivo selecionado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /* 
        Método: capitalizeFirstLetter
        Finalidade: Receber uma string e retornar ela com a primeira letra maiúscula,
                   mantendo o resto igual.
        Contexto: Usado para formatar mensagens ou palavras, deixando mais apresentável na interface.
     */
    private String capitalizeFirstLetter(String s) {
        if (s == null || s.isEmpty()) {
            return s; // Se a string for nula ou vazia, retorna como está
        }
        // Pega a primeira letra e deixa maiúscula, concatena com o resto da string
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /* 
        Método: showErrorLogViews
        Finalidade: Exibir na interface a visualização dos logs de erro, organizando as tabelas de erros e ambiente.
        Contexto: Usado para trocar a view central do programa para mostrar o conteúdo específico dos arquivos error.log,
                  colocando as tabelas de erro e ambiente empilhadas verticalmente.
     */
    private void showErrorLogViews() {
        // Pega o componente central da janela (espera que seja um JPanel)
        Component centerComp = getContentPane().getComponent(1);

        if (centerComp instanceof JPanel) {
            JPanel panel = (JPanel) centerComp;
            panel.removeAll(); // Limpa tudo que estava lá

            // Define layout vertical para empilhar componentes
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            // Adiciona a tabela de erros dentro de uma barra de rolagem
            panel.add(new JScrollPane(errorTable));

            // Adiciona a tabela de ambiente também com barra de rolagem
            panel.add(new JScrollPane(envTable));

            // Atualiza o painel para refletir as mudanças na interface
            panel.revalidate();
            panel.repaint();
        }
    }

    /* 
        Método: showLogProfilerViews
        Finalidade: Configurar a interface para exibir as tabelas principais de logs e a pilha de chamadas.
        Contexto: Utilizado para mostrar os dados dos arquivos console.log e SHIFT+F6, com as views de logs
                  e call stack empilhadas verticalmente na área principal da janela.
     */
    private void showLogProfilerViews() {
        // Pega o componente central da janela (espera que seja um JPanel)
        Component centerComp = getContentPane().getComponent(1);

        if (centerComp instanceof JPanel) {
            JPanel panel = (JPanel) centerComp;
            panel.removeAll(); // Limpa componentes antigos

            // Define layout vertical para empilhar componentes
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            // Adiciona tabela principal de logs com scroll
            panel.add(new JScrollPane(logTable));

            // Adiciona tabela da pilha de chamadas com scroll
            panel.add(new JScrollPane(callStackTable));

            // Atualiza o painel para refletir as mudanças
            panel.revalidate();
            panel.repaint();
        }
    }

    /* 
        Método: toggleDarkMode
        Finalidade: Alternar entre o modo escuro e claro da interface, atualizando o botão e aplicando o tema.
        Contexto: Chamado quando o usuário clica no botão para trocar o tema visual do programa.
     */
    private void toggleDarkMode() {
        darkMode = !darkMode; // Inverte o estado atual do modo escuro
        // Atualiza o texto do botão para refletir o próximo modo disponível
        darkModeButton.setText(darkMode ? "Modo Claro" : "Modo Dark");
        applyTheme(); // Aplica o tema correspondente ao estado atual
    }

    /* 
        Método: applyTheme
        Finalidade: Aplicar as cores e estilos da interface de acordo com o modo (dark mode ou light mode).
        Contexto: Usado para atualizar visualmente todos os componentes da janela quando o tema muda.
     */
    private void applyTheme() {
        // Define cores de fundo e texto conforme modo escuro ou claro
        Color bgColor = darkMode ? Color.decode("#121212") : Color.WHITE;
        Color fgColor = darkMode ? Color.WHITE : Color.BLACK;
        Color tableBg = darkMode ? Color.decode("#1E1E1E") : Color.WHITE;
        Color tableFg = fgColor;
        Color selectionBg = darkMode ? Color.decode("#333333") : new Color(184, 207, 229);

        // Aplica cores no painel principal e labels de status
        getContentPane().setBackground(bgColor);
        tipoArquivoLabel.setForeground(darkMode ? Color.CYAN : Color.BLUE);
        footerLabel.setForeground(fgColor);

        // Aplica cores e seleção na tabela principal de logs
        logTable.setBackground(tableBg);
        logTable.setForeground(tableFg);
        logTable.setSelectionBackground(selectionBg);
        logTable.setSelectionForeground(tableFg);

        // Mesma coisa para a tabela da pilha de chamadas
        callStackTable.setBackground(tableBg);
        callStackTable.setForeground(tableFg);
        callStackTable.setSelectionBackground(selectionBg);
        callStackTable.setSelectionForeground(tableFg);

        // Renderers customizados para alinhar texto e aplicar estilos
        CallStackRenderer callStackRenderer = new CallStackRenderer(selectionBg, tableFg, tableFg);

        callStackTable.getColumnModel().getColumn(0).setCellRenderer(callStackRenderer);
        callStackTable.getColumnModel().getColumn(1).setCellRenderer(callStackRenderer);
        callStackTable.getColumnModel().getColumn(2).setCellRenderer(callStackRenderer);

        // Ajusta fonte e altura das linhas da tabela call stack
        callStackTable.setFont(new Font("Monospaced", Font.PLAIN, 13));
        callStackTable.setRowHeight(24);

        // Aplica cores e seleção nas tabelas de erro e ambiente
        errorTable.setBackground(tableBg);
        errorTable.setForeground(tableFg);
        errorTable.setSelectionBackground(selectionBg);
        errorTable.setSelectionForeground(tableFg);

        envTable.setBackground(tableBg);
        envTable.setForeground(tableFg);
        envTable.setSelectionBackground(selectionBg);
        envTable.setSelectionForeground(tableFg);

        // Atualiza os componentes do header (barra superior) para seguir o tema
        Component headerPanel = ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (headerPanel instanceof JPanel) {
            for (Component c : ((JPanel) headerPanel).getComponents()) {
                updateComponentTheme(c, bgColor, fgColor);
                if (c instanceof JPanel) {
                    for (Component cc : ((JPanel) c).getComponents()) {
                        updateComponentTheme(cc, bgColor, fgColor);
                    }
                }
            }
        }

        // Pede para a interface redesenhar tudo já com o tema novo
        repaint();
    }

    /* 
        Método: updateComponentTheme
        Finalidade: Atualizar as cores de fundo e texto de um componente Swing e seus filhos recursivamente,
                   respeitando o tema dark ou light.
        Contexto: Chamado durante a aplicação do tema para deixar a interface toda consistente visualmente.
     */
    private void updateComponentTheme(Component c, Color bg, Color fg) {
        if (c instanceof JPanel) {
            // Painel: muda background e atualiza todos os componentes filhos
            c.setBackground(bg);
            for (Component cc : ((JPanel) c).getComponents()) {
                updateComponentTheme(cc, bg, fg);
            }
        } else if (c instanceof JLabel) {
            // Label: muda cor do texto (foreground) e fundo (background)
            c.setForeground(fg);
            c.setBackground(bg);
        } else if (c instanceof JButton) {
            // Botão: muda texto e fundo (fundo especial no modo escuro, null no claro)
            c.setForeground(fg);
            c.setBackground(darkMode ? Color.decode("#333333") : null);
        } else if (c instanceof JTextField) {
            // Campo de texto: muda texto, fundo e cor do cursor
            c.setForeground(fg);
            c.setBackground(darkMode ? Color.decode("#222222") : Color.WHITE);
            ((JTextField) c).setCaretColor(fg);
        }
    }

    /* 
        Método: parseAndPopulateCallstack
        Finalidade: Processar linhas de log que indicam pilha de chamadas (call stack),
                   extrair dados da rotina, fonte, data/hora e linha, e popular as tabelas da UI.
        Contexto: Usado para interpretar blocos de logs com linhas iniciadas por "Called from",
                  construindo a visualização das chamadas na interface.
     */
    private void parseAndPopulateCallstack(List<String> lines) {
        int order = 1; // Contador para numerar as chamadas na tabela principal

        for (String line : lines) {
            line = line.trim();

            // Só processa linhas que começam com "Called from"
            if (line.startsWith("Called from")) {
                try {
                    // Remove o texto fixo "Called from" para facilitar o parsing
                    String raw = line.replace("Called from", "").trim();

                    // Extrai a rotina (antes do primeiro parêntese) ou tudo se não tiver
                    String rotina = raw.contains("(") ? raw.substring(0, raw.indexOf("(")).trim() : raw;

                    // Extrai o nome do arquivo fonte dentro dos parênteses, se existir
                    String fonte = raw.contains("(") ? raw.substring(raw.indexOf("(") + 1, raw.indexOf(")")) : "-";

                    // Extrai a data/hora entre o fechamento do parêntese e "line :", se existir
                    String dataHora = raw.contains(")") && raw.contains("line :") ? raw.substring(raw.indexOf(")") + 1, raw.indexOf("line :")).trim() : "";

                    // Extrai o número da linha depois de "line :", se existir
                    String linha = raw.contains("line :") ? raw.substring(raw.indexOf("line :") + 7).trim() : "";

                    // Adiciona os dados extraídos na tabela principal de logs
                    logTableModel.addRow(new Object[]{
                        String.format("# %06d", order++),
                        rotina,
                        fonte
                    });

                    // Prepara a linha para adicionar na lista da pilha de chamadas (tabela inferior)
                    Vector<String> row = new Vector<>();
                    row.add(rotina);
                    row.add(fonte);
                    row.add(dataHora);
                    row.add(linha);

                    fullCallStackRows.add(row);

                } catch (Exception ex) {
                    // Caso ocorra algum erro no parsing, ignora e segue em frente
                }
            }
        }
    }

    /* 
        Método: parseCallLine
        Finalidade: Interpretar uma linha de log que começa com "CALL", extraindo o nome da função,
                   o arquivo fonte e o resto da linha opcional.
        Contexto: Usado para processar logs de chamadas no formato específico do Protheus, para preencher tabelas.
     */
    private Vector<String> parseCallLine(String line) {
        Vector<String> row = new Vector<>();
        try {
            System.out.println("[parseCallLine] Linha: '" + line + "'");

            // Limpa espaços extras e deixa a linha organizada para regex
            String clean = line.replaceAll("\\s+", " ").trim();

            // Regex para capturar: CALL + função + (fonte) + resto opcional da linha
            Pattern pattern = Pattern.compile("CALL\\s+([\\w#:.]+)\\s*\\(([^)]+)\\)\\s*(.*)");
            Matcher matcher = pattern.matcher(clean);

            if (matcher.find()) {
                // Pega nome da função (grupo 1)
                String funcName = matcher.group(1).trim();

                // Pega arquivo fonte (grupo 2)
                String source = matcher.group(2).trim();

                // Pega o resto da linha após a fonte (grupo 3)
                String extra = matcher.group(3).trim();

                row.add(funcName);
                row.add(source);
                row.add(extra);
            } else {
                // Caso a linha não bata com o padrão esperado, marca como erro
                row.add("Erro");
                row.add("-");
                row.add("-");
            }
        } catch (Exception e) {
            // Em caso de erro inesperado, também marca erro na linha
            row.add("Erro");
            row.add("-");
            row.add("-");
        }
        return row;
    }

    /* 
        Método: parseFromLine
        Finalidade: Interpretar linhas de log que indicam chamadas do tipo "-- FROM",
                   extraindo rotina, arquivo fonte e número da linha.
        Contexto: Usado para analisar logs de pilha de chamadas, aceitando dois formatos diferentes
                  (legacy e novo), e retornar os dados para popular as tabelas do programa.
     */
    private Vector<String> parseFromLine(String line) {
        Vector<String> row = new Vector<>();
        try {
            // Debug: mostra a linha original para ajudar a rastrear problemas
            System.out.println("[parseFromLine] Linha original: '" + line + "'");

            // Encontra a posição da substring "-- FROM"
            int fromIndex = line.indexOf("-- FROM");
            if (fromIndex == -1) {
                throw new IllegalArgumentException("Linha inválida, não contém -- FROM: " + line);
            }

            // Verifica se a linha tem tamanho mínimo após "-- FROM"
            if (line.length() < fromIndex + 7) { // 7 é o tamanho de "-- FROM"
                throw new IllegalArgumentException("Linha muito curta após -- FROM: " + line);
            }

            // Extrai a parte relevante da linha, limpa espaços extras
            String clean = line.substring(fromIndex).replaceAll("\\s+", " ").trim();

            System.out.println("[parseFromLine] Linha após limpeza: '" + clean + "'");

            // Padrão antigo: -- FROM ROTINA (FONTE) (LINHA)
            Pattern legacyPattern = Pattern.compile("-- FROM ([\\w:#]+(?:[:.]?[\\w]+)*) \\(([^)]+)\\) \\((\\d+)\\)");
            Matcher legacyMatcher = legacyPattern.matcher(clean);

            if (legacyMatcher.find()) {
                row.add(legacyMatcher.group(1).trim()); // rotina
                row.add(legacyMatcher.group(2).trim()); // fonte
                row.add(legacyMatcher.group(3).trim()); // linha
                return row;
            }

            // Padrão novo: -- FROM ROTINA (FONTE) LN LINHA
            Pattern newPattern = Pattern.compile("-- FROM ([\\w:#]+(?:[:.]?[\\w]+)*) \\(([^)]+)\\) LN (\\d+)");
            Matcher newMatcher = newPattern.matcher(clean);

            if (newMatcher.find()) {
                row.add(newMatcher.group(1).trim()); // rotina
                row.add(newMatcher.group(2).trim()); // fonte
                row.add(newMatcher.group(3).trim()); // linha
                return row;
            }

            System.err.println("⚠️ FROM não reconhecido no parseFromLine: '" + clean + "'");

        } catch (Exception e) {
            System.err.println("[parseFromLine] Erro: " + e.getMessage());
        }

        // Caso não consiga fazer o parse, retorna uma linha padrão indicando erro
        row.add("Erro");
        row.add("-");
        row.add("0");
        return row;
    }

    /* 
        Método: main
        Finalidade: Ponto de entrada da aplicação Java, que inicia a interface gráfica LogProfilerView.
        Contexto: Usa SwingUtilities.invokeLater para garantir que a criação da GUI aconteça na thread correta do Swing.
     */
    public static void main(String[] args) {
        // Garante que o GUI seja criado na Event Dispatch Thread do Swing
        SwingUtilities.invokeLater(() -> new LogProfilerView().setVisible(true));
    }

}
