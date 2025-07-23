/* ==============================================================
Autor: Edison Luiz
Ajuda: ChatGPT
Versão: V2.1 Alpha

Descrição:
* Interface gráfica em Java para auxiliar na leitura de arquivos
de log do Protheus, como LogProfiler, Console.log contendo Log
Profiler e Error.log.

Updates:
* Removido o Dark Mode (não me agradou...);
* Corrigida a exibição dos filtros. Ao filtrar e clicar em um 
registro, seu callstack agora é exibido corretamente;
* Ao abrir o programa, a janela aparece centralizada na área de
trabalho;
* Adicionado campo para exibir o nome do arquivo aberto em tela.
* Ao abrir um arquivo, o título da janela é alterado também.

GitHub: https://github.com/edisoncake
============================================================== */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.MalformedInputException;
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
    private JTable envTable, middleTable;
    private DefaultTableModel envTableModel, middleTableModel;
    private JTextField searchLogField;
    private JLabel tipoArquivoLabelLogs, nomeArquivoLabelLogs;
    private JLabel tipoArquivoLabelErros, nomeArquivoLabelErros;
    private JLabel footerLabel;
    private List<Vector<String>> fullCallStackRows = new ArrayList<>();
    private boolean showingCustomOnly = false;
    private boolean showingInternalOnly = false;
    private JButton toggleCustomButton;
    private JButton toggleInternalButton;
    private JTabbedPane tabbedPane;
    private Map<String, CallBlockInfo> shiftF6CallMap = new LinkedHashMap<>();
    private Map<String, CallBlockInfo> consoleLogCallMap = new HashMap<>();
    private boolean isConsoleLog = false;
    private boolean isShiftF6Log = false;
    private JTextArea errorPromptArea;
    private JTextArea queryTextArea;
    private JTabbedPane errorSubTabbedPane;
    private JPanel middleTablePanel;
    private boolean isErrorLog = false;

    /**
     * Representa uma entrada da call stack com informações de origem,
     * posição no código e métricas de chamadas.
     */
    private static class CallStackEntry {
        private final String calledFrom; // Origem da chamada (quem chamou)
        private final String sourceFile; // Nome do arquivo fonte da rotina
        private final int lineNumber; // Linha no arquivo fonte
        private final int numCalls; // Número total de chamadas dessa entrada
        private final double totalTime; // Tempo total gasto nessa chamada
        private final double maxTime; // Tempo máximo gasto em uma única chamada

        // Construtor que inicializa todos os campos
        public CallStackEntry(String calledFrom, String sourceFile, int lineNumber, int numCalls, double totalTime,
                double maxTime) {
            this.calledFrom = calledFrom;
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
            this.numCalls = numCalls;
            this.totalTime = totalTime;
            this.maxTime = maxTime;
        }

        // Getters para acesso aos campos
        public String getCalledFrom() {
            return calledFrom;
        }

        public String getSourceFile() {
            return sourceFile;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getNumCalls() {
            return numCalls;
        }

        public double getTotalTime() {
            return totalTime;
        }

        public double getMaxTime() {
            return maxTime;
        }
    }

    /**
     * Renderer customizado para a callstack, que formata texto e cores
     * conforme o conteúdo e estado de seleção da célula.
     */
    class CallStackRenderer extends JLabel implements TableCellRenderer {

        private final Color selectionBg; // Cor de fundo quando selecionado
        private final Color selectionFg; // Cor do texto quando selecionado
        private final Color defaultFg; // Cor padrão do texto

        // Construtor que configura cores, alinhamento e fonte monoespaçada
        public CallStackRenderer(Color selectionBg, Color selectionFg, Color defaultFg) {
            setOpaque(true); // Para pintar o fundo da célula
            this.selectionBg = selectionBg;
            this.selectionFg = selectionFg;
            this.defaultFg = defaultFg;

            setVerticalAlignment(SwingConstants.CENTER); // Centraliza verticalmente
            setHorizontalAlignment(SwingConstants.CENTER); // Centraliza horizontalmente
            setFont(new Font("Monospaced", Font.PLAIN, 13)); // Fonte monoespaçada
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {

            String text = (value != null) ? value.toString() : ""; // Texto da célula

            Color fg;
            if (text.startsWith("U_")) {
                fg = Color.BLUE; // Rotinas customizadas
            } else if (text.contains("{||")) {
                fg = new Color(0, 200, 0); // Blocos de código (lambda ou anônimo)
            } else if (text.startsWith("#")) {
                fg = new Color(0, 200, 0); // Comentários/metatags
            } else {
                fg = isSelected ? selectionFg : defaultFg; // Cor padrão ou seleção
            }

            setForeground(fg);
            setBackground(isSelected ? selectionBg : table.getBackground());

            // Usa HTML para múltiplas linhas e centralização
            String escapedText = escapeHtml(text).replace("\n", "<br>");
            setText("<html><div style='text-align: center;'>" + escapedText + "</div></html>");

            // Ajusta altura da linha para o conteúdo
            int preferredHeight = getPreferredSize().height;
            if (table.getRowHeight(row) != preferredHeight) {
                table.setRowHeight(row, preferredHeight);
            }

            return this;
        }

        // Escapa caracteres HTML especiais para evitar renderização incorreta
        private String escapeHtml(String s) {
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }

    /**
     * Renderizador customizado para células da tabela que permite
     * múltiplas linhas com quebra automática de texto e aplica cor azul
     * para rotinas que começam com "U_".
     */
    class CustomCallRenderer extends JTextArea implements TableCellRenderer {

        // Construtor que ativa quebra de linha e fundo opaco
        public CustomCallRenderer() {
            setLineWrap(true); // Quebra automática de linhas
            setWrapStyleWord(true); // Quebra por palavra (não no meio)
            setOpaque(true); // Permite pintar o fundo
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            setText(value != null ? value.toString() : ""); // Define texto da célula (evita null)

            String nome = table.getModel().getValueAt(table.convertRowIndexToModel(row), 1).toString(); // Valor coluna
                                                                                                        // "Nome"

            if (nome.startsWith("U_")) {
                setForeground(Color.BLUE); // Cor azul para rotinas customizadas
            } else {
                setForeground(isSelected ? table.getSelectionForeground() : table.getForeground()); // Cor padrão ou
                                                                                                    // seleção
            }

            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground()); // Fundo conforme
                                                                                                // seleção

            setFont(table.getFont()); // Usa fonte da tabela

            // Ajusta altura da linha conforme conteúdo renderizado
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
            int preferredHeight = getPreferredSize().height;
            if (table.getRowHeight(row) != preferredHeight) {
                table.setRowHeight(row, preferredHeight);
            }

            return this; // Retorna o componente preparado
        }
    }

    /**
     * Construtor da classe LogProfilerView.
     * Responsável por montar a interface gráfica principal do aplicativo,
     * incluindo abas, tabelas, botões, labels e eventos associados.
     * Estrutura a UI com duas abas principais: "Logs" e "Error Logs",
     * cada uma com seus controles e informações específicas.
     */
    public LogProfilerView() {
        // Configurações básicas da janela principal
        setTitle("LogProfiler View (Java Edition)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLayout(new BorderLayout());

        // ---------- COMPONENTES DE INTERFACE PARA LABELS DE STATUS ----------
        // Labels para aba "Logs" que exibem o tipo de arquivo carregado e o nome do
        // arquivo
        tipoArquivoLabelLogs = new JLabel("Tipo: Nenhum arquivo carregado");
        tipoArquivoLabelLogs.setForeground(Color.BLUE);
        nomeArquivoLabelLogs = new JLabel("Arquivo: (nenhum)");
        nomeArquivoLabelLogs.setForeground(Color.DARK_GRAY);

        // Labels para aba "Error Logs", mesmos propósitos da aba "Logs"
        tipoArquivoLabelErros = new JLabel("Tipo: Nenhum arquivo carregado");
        tipoArquivoLabelErros.setForeground(Color.BLUE);
        nomeArquivoLabelErros = new JLabel("Arquivo: (nenhum)");
        nomeArquivoLabelErros.setForeground(Color.DARK_GRAY);

        // ---------- TOPO DA ABA LOGS ----------
        // Botão para abrir arquivos de log na aba "Logs"
        JButton openFileButton = new JButton("Abrir Arquivo Log");

        // Painel superior que agrupa o botão e as labels de status na aba "Logs"
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(openFileButton);
        topBar.add(tipoArquivoLabelLogs);
        topBar.add(nomeArquivoLabelLogs);

        // Painel de filtros para busca dentro dos logs
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLogField = new JTextField(30); // Campo texto para filtro
        JButton clearButton = new JButton("Limpar Filtros"); // Botão para limpar filtros
        toggleCustomButton = new JButton("Exibir Customizados"); // Toggle filtro customizado
        toggleInternalButton = new JButton("Exibir Internos"); // Toggle filtro interno

        // Adiciona componentes ao painel de filtros
        filterPanel.add(new JLabel("Filtro Log: "));
        filterPanel.add(searchLogField);
        filterPanel.add(clearButton);
        filterPanel.add(toggleCustomButton);
        filterPanel.add(toggleInternalButton);

        // Define tamanho máximo do painel de filtros para manter layout agradável
        filterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Painel que junta o topo e o filtro verticalmente na aba "Logs"
        JPanel logsHeaderPanel = new JPanel();
        logsHeaderPanel.setLayout(new BoxLayout(logsHeaderPanel, BoxLayout.Y_AXIS));
        logsHeaderPanel.add(topBar);
        logsHeaderPanel.add(filterPanel);

        // ---------- TABELAS PARA LOGS ----------
        // Modelo da tabela principal de logs com colunas específicas
        logTableModel = new DefaultTableModel(
                new String[] { "# Ordem", "Nome", "Fonte", "Número de Chamadas", "Tempo Máximo (ms)" }, 0);

        // Modelo para callstack que exibe detalhes básicos
        callStackTableModel = new DefaultTableModel(
                new String[] { "Chamado de", "Fonte", "Linha", "Chamadas (C)", "Tempo Total (T)", "Tempo Máximo (M)" },
                0);

        // Tabelas concretas baseadas nos modelos acima
        logTable = new JTable(logTableModel);
        callStackTable = new JTable(callStackTableModel);

        // Habilita ordenação automática na tabela de logs
        logTable.setAutoCreateRowSorter(true);

        // Aplica renderizador customizado para a coluna "Nome" (índice 1)
        logTable.getColumnModel().getColumn(1).setCellRenderer(new CustomCallRenderer());

        // Ajusta largura das colunas para melhor visualização
        logTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(140);

        // Bloqueia o rearranjo e o redimensionamento das colunas da tabela de logs
        logTable.getTableHeader().setReorderingAllowed(false);
        logTable.getTableHeader().setResizingAllowed(false);

        // Listener para atualização da callstack ao selecionar uma linha na tabela de
        // logs
        logTable.getSelectionModel().addListSelectionListener(e -> {
            // Garante que o evento seja apenas finalizado e que não esteja na aba de erros
            if (!e.getValueIsAdjusting() && !isErrorLog) {
                int selectedRow = logTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < logTable.getRowCount()) {
                    int modelRow = logTable.convertRowIndexToModel(selectedRow);
                    // Obtém o nome da função selecionada, já formatado (uppercase + trim)
                    String funcName = logTableModel.getValueAt(modelRow, 1).toString().toUpperCase().trim();

                    // Atualiza a callstack com base no tipo de log aberto
                    if (isShiftF6Log) {
                        updateCallStackShiftF6(funcName, shiftF6CallMap);
                    } else if (isConsoleLog) {
                        updateCallStack(funcName);
                    }
                }
            }
        });

        // Painel que contém as duas tabelas empilhadas verticalmente (logs + callstack)
        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
        logPanel.add(new JScrollPane(logTable));
        logPanel.add(new JScrollPane(callStackTable));

        // Painel final da aba "Logs" que agrupa cabeçalho e tabelas
        JPanel fullLogPanel = new JPanel(new BorderLayout());
        fullLogPanel.add(logsHeaderPanel, BorderLayout.NORTH);
        fullLogPanel.add(logPanel, BorderLayout.CENTER);

        // ---------- COMPONENTES DA ABA ERROR LOG ----------
        // Botão para abrir arquivos de log de erro
        JButton openErrorLogButton = new JButton("Abrir Error.log");

        // Painel superior da aba de erros com botão e labels de status
        JPanel errorTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        errorTopPanel.add(openErrorLogButton);
        errorTopPanel.add(tipoArquivoLabelErros);
        errorTopPanel.add(nomeArquivoLabelErros);

        // Área de texto para mostrar mensagens de erro detalhadas, configurada para
        // leitura
        errorPromptArea = new JTextArea(7, 100);
        errorPromptArea.setEditable(false);
        errorPromptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        errorPromptArea.setCaretPosition(0);
        errorPromptArea.setLineWrap(false);
        errorPromptArea.setWrapStyleWord(false);

        // Painel que envolve a área de erro com título "Erro"
        JPanel errorPromptPanel = new JPanel(new BorderLayout());
        errorPromptPanel.setBorder(BorderFactory.createTitledBorder("Erro"));
        errorPromptPanel.add(new JScrollPane(errorPromptArea), BorderLayout.CENTER);

        // Tabela para exibir informações do ambiente (variáveis, configs, etc)
        envTableModel = new DefaultTableModel(new String[] { "Campo", "Valor" }, 0);
        envTable = new JTable(envTableModel);
        JPanel envTablePanel = new JPanel(new BorderLayout());
        envTablePanel.setBorder(BorderFactory.createTitledBorder("Informações do Ambiente"));
        envTablePanel.add(new JScrollPane(envTable), BorderLayout.CENTER);

        // Tabela para exibir callstack específico de error.log
        middleTableModel = new DefaultTableModel(
                new String[] { "Called From", "Rotina (Fonte)", "Data do Fonte", "Linha" }, 0);
        middleTable = new JTable(middleTableModel);
        middleTablePanel = new JPanel(new BorderLayout());
        middleTablePanel.setBorder(BorderFactory.createTitledBorder("Callstack do Error.log (se houver)"));
        middleTablePanel.add(new JScrollPane(middleTable), BorderLayout.CENTER);

        // Área de texto para exibir queries SQL extraídas do error.log
        queryTextArea = new JTextArea(10, 100);
        queryTextArea.setEditable(false);
        queryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        queryTextArea.setLineWrap(true);
        queryTextArea.setWrapStyleWord(true);
        queryTextArea.setCaretPosition(0);

        // Painel que envolve a área de query com título "Query SQL"
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.setBorder(BorderFactory.createTitledBorder("Query SQL"));
        queryPanel.add(new JScrollPane(queryTextArea), BorderLayout.CENTER);

        // Painel com abas internas dentro da aba Error Logs para organizar erros,
        // ambiente, callstack e query
        errorSubTabbedPane = new JTabbedPane();
        errorSubTabbedPane.addTab("Erro", errorPromptPanel);
        errorSubTabbedPane.addTab("Ambiente", envTablePanel);
        errorSubTabbedPane.addTab("Callstack", middleTablePanel);
        errorSubTabbedPane.addTab("Query SQL", queryPanel);

        // Painel final da aba Error Logs que junta topo e abas internas
        JPanel fullErrorPanel = new JPanel(new BorderLayout());
        fullErrorPanel.add(errorTopPanel, BorderLayout.NORTH);
        fullErrorPanel.add(errorSubTabbedPane, BorderLayout.CENTER);

        // ---------- CONFIGURAÇÃO DAS ABAS PRINCIPAIS ----------
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Logs", fullLogPanel);
        tabbedPane.addTab("Error Logs", fullErrorPanel);
        add(tabbedPane, BorderLayout.CENTER);

        // ---------- RODAPÉ ----------
        footerLabel = new JLabel("© Edison Luiz - v3.0 (ALPHA)   ||   (⌐■_■)ノ💄👠✨ ~ Gaga vibes", JLabel.CENTER);
        footerLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(footerLabel, BorderLayout.SOUTH);

        // ---------- CONFIGURAÇÃO DE AÇÕES ----------
        // Associa o botão "Abrir Arquivo Log" ao método de abrir arquivos de log
        // normais
        openFileButton.addActionListener(this::handleFileOpen);

        // Associa o botão "Abrir Error.log" ao método específico para abrir logs de
        // erro
        openErrorLogButton.addActionListener(this::handleErrorLogOpen);

        // Listener que monitora mudanças no campo de busca para filtrar a tabela em
        // tempo real
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

        // Botão para limpar filtros e resetar estados de exibição
        clearButton.addActionListener(e -> {
            searchLogField.setText("");
            showingCustomOnly = false;
            toggleCustomButton.setText("Exibir Customizados");
            showingInternalOnly = false;
            toggleInternalButton.setText("Exibir Internos");
            filterLog();
        });

        // Toggle para mostrar somente logs customizados
        toggleCustomButton.addActionListener(e -> {
            showingCustomOnly = !showingCustomOnly;
            if (showingCustomOnly) {
                showingInternalOnly = false;
                toggleInternalButton.setText("Exibir Internos");
            }
            toggleCustomButton.setText(showingCustomOnly ? "Resetar" : "Exibir Customizados");
            filterLog();
        });

        // Toggle para mostrar somente logs internos
        toggleInternalButton.addActionListener(e -> {
            showingInternalOnly = !showingInternalOnly;
            if (showingInternalOnly) {
                showingCustomOnly = false;
                toggleCustomButton.setText("Exibir Customizados");
            }
            toggleInternalButton.setText(showingInternalOnly ? "Resetar" : "Exibir Internos");
            filterLog();
        });

        // Aplica tema customizado à interface (método externo)
        applyTheme();

        // Centraliza a janela na tela
        setLocationRelativeTo(null);
    }

    /**
     * Atualiza as labels que exibem o tipo do arquivo e o nome do arquivo
     * carregado, tanto na aba "Logs" quanto na aba "Error Logs".
     * 
     * @param tipo String representando o tipo do arquivo (ex: "LogProfiler",
     *             "Error.log")
     * @param nome String com o nome do arquivo aberto
     */
    private void setFileLabels(String tipo, String nome) {
        // Atualiza label de tipo na aba Logs
        tipoArquivoLabelLogs.setText(tipo);
        // Atualiza label de nome na aba Logs
        nomeArquivoLabelLogs.setText(nome);

        // Atualiza label de tipo na aba Error Logs
        tipoArquivoLabelErros.setText(tipo);
        // Atualiza label de nome na aba Error Logs
        nomeArquivoLabelErros.setText(nome);
    }

    /**
     * Lê e processa um arquivo console.log, extraindo blocos de chamadas
     * e atualizando a interface com as informações carregadas.
     * Exibe erro se o trecho esperado não for encontrado.
     *
     * @param file Arquivo console.log a ser lido.
     * @throws IOException Erro na leitura do arquivo.
     */
    private void readConsoleLogFile(File file) throws IOException {
        // Limpa estados e visões anteriores para começar do zero
        resetAllViews();
        isConsoleLog = true;

        // Lista para armazenar todas as linhas do arquivo
        List<String> lines = new ArrayList<>();
        // Leitura do arquivo usando encoding ISO-8859-1
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // Procura o índice do último começo de trecho APP PROFILER no arquivo
        int lastProfilerStart = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).contains("--- BEGIN APP PROFILER")) {
                lastProfilerStart = i;
                break;
            }
        }

        // Caso não encontre o trecho esperado, mostra alerta e retorna
        if (lastProfilerStart == -1) {
            JOptionPane.showMessageDialog(null,
                    "Nenhum trecho de APP PROFILER encontrado no console.log",
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Limpa mapa de chamadas do log console para nova carga
        consoleLogCallMap.clear();

        // Pega o sub-lista que vai do último início do profiler até o fim do arquivo
        List<String> trechoFinal = lines.subList(lastProfilerStart, lines.size());

        int ordem = 1; // Contador para ordem dos blocos de chamada

        // Lista auxiliar para acumular linhas de um bloco de chamada
        List<String> callBlockLines = new ArrayList<>();

        // Processa linha a linha o trecho final, agrupando blocos de chamadas
        for (String rawLine : trechoFinal) {
            // Limpa e remove espaços desnecessários
            String linha = cleanLine(rawLine).trim();

            if (linha.startsWith("CALL")) {
                // Se já tem linhas acumuladas, processa o bloco anterior antes
                if (!callBlockLines.isEmpty()) {
                    processCallBlock(callBlockLines, ordem++, true, consoleLogCallMap);
                    callBlockLines.clear();
                }
                callBlockLines.add(linha);
            } else if (linha.startsWith("-- FROM")) {
                // Adiciona linhas que indicam origem da chamada ao bloco atual
                callBlockLines.add(linha);
            } else {
                // Qualquer outra linha finaliza o bloco atual, se existir
                if (!callBlockLines.isEmpty()) {
                    processCallBlock(callBlockLines, ordem++, true, consoleLogCallMap);
                    callBlockLines.clear();
                }
            }
        }

        // Caso sobre um bloco não processado no fim, processa ele também
        if (!callBlockLines.isEmpty()) {
            processCallBlock(callBlockLines, ordem++, true, consoleLogCallMap);
        }

        // Atualiza visualizações da interface com os dados carregados
        showLogProfilerViews();

        // Seleciona a primeira linha da tabela de logs, se existir
        if (logTableModel.getRowCount() > 0) {
            logTable.setRowSelectionInterval(0, 0);
        }

        // Garante que a aba "Logs" esteja selecionada para visualização
        tabbedPane.setSelectedIndex(0);
    }

    /*
     * Método: filterLog
     * Finalidade: Aplicar filtros dinâmicos na tabela de logs (logTable) com base
     * no texto digitado e
     * nas opções de exibição (customizados e internos), atualizando a exibição dos
     * dados.
     * Contexto: Usado toda vez que o usuário digita no campo de busca ou alterna os
     * botões de filtro,
     * para facilitar encontrar funções específicas no log.
     */
    private void filterLog() {
        // Pega o sorter da tabela para aplicar filtros
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) logTable.getRowSorter();

        // Pega o texto do campo de busca e remove espaços extras
        String text = searchLogField.getText().trim();

        // Lista que vai armazenar os filtros ativos
        java.util.List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

        // Filtro principal de texto (nome da função OU callstack)
        if (!text.isEmpty()) {
            RowFilter<DefaultTableModel, Object> combinedFilter = new RowFilter<DefaultTableModel, Object>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                    String funcName = entry.getStringValue(1).toUpperCase();

                    if (funcName.contains(text.toUpperCase())) {
                        return true;
                    }

                    CallBlockInfo info = isShiftF6Log
                            ? shiftF6CallMap.get(funcName)
                            : isConsoleLog ? consoleLogCallMap.get(funcName) : null;

                    List<CallStackEntry> callstack = (info != null) ? info.callStackEntries : null;

                    if (callstack != null) {
                        for (CallStackEntry entryCall : callstack) {
                            if (entryCall.getCalledFrom().toUpperCase().contains(text.toUpperCase())
                                    || entryCall.getSourceFile().toUpperCase().contains(text.toUpperCase())
                                    || String.valueOf(entryCall.getLineNumber()).contains(text)) {
                                return true;
                            }
                        }
                    }

                    return false;
                }
            };

            filters.add(combinedFilter);
        }

        // Filtro: só mostrar funções customizadas
        if (showingCustomOnly) {
            filters.add(RowFilter.regexFilter("^U_", 1)); // coluna 1 = nome da função
        }

        // Filtro: só mostrar internos (na coluna Fonte)
        if (showingInternalOnly) {
            filters.add(RowFilter.regexFilter("(?i)internal", 2)); // coluna 2 = fonte
        }

        // Aplica os filtros combinados (AND) ou limpa se não houver filtro
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
            callStackTableModel.setRowCount(0); // Limpa a pilha quando não filtra nada
        } else if (filters.size() == 1) {
            sorter.setRowFilter(filters.get(0)); // Aplica único filtro
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters)); // Aplica todos juntos
        }
    }

    /**
     * Abre um arquivo de log via JFileChooser e processa seu conteúdo
     * para identificar se é um log Console, SHIFT+F6, ou inválido.
     * Atualiza as flags de tipo, limpa dados anteriores e carrega os dados novos.
     */
    private void handleFileOpen(ActionEvent e) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // Aplica look-and-feel nativo
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this); // Abre diálogo para selecionar arquivo

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            setTitle("Log Profiler View - " + selectedFile.getName()); // Atualiza título com nome do arquivo

            try {
                List<String> linhas = Files.readAllLines(selectedFile.toPath(), StandardCharsets.ISO_8859_1); // Lê
                                                                                                              // linhas
                                                                                                              // com
                                                                                                              // encoding
                                                                                                              // ISO-8859-1

                boolean foundShiftF6 = false;
                boolean foundConsoleLog = false;
                boolean hasBegin = false, hasMemory = false, hasEnd = false;

                for (String linha : linhas) {
                    if (linha.trim().isEmpty())
                        continue; // Ignora linhas vazias

                    if (linha.contains("/* ========================================")) {
                        foundShiftF6 = true; // Detecta log SHIFT+F6
                    }
                    if (foundShiftF6 && linha.contains("Request Profiler Log")) {
                        foundShiftF6 = true; // Confirma SHIFT+F6
                    }
                    if (linha.contains("--- BEGIN APP PROFILER ( THREAD [")) {
                        hasBegin = true; // Parte inicial log Console
                    }
                    if (linha.contains("--- MEMORY LOG PROFILER ---")) {
                        hasMemory = true; // Parte memória log Console
                    }
                    if (linha.contains("--- END APP PROFILER ---")) {
                        hasEnd = true; // Parte final log Console
                    }
                }

                foundConsoleLog = hasBegin && hasMemory && hasEnd; // Valida log Console

                // Reseta flags e limpa tabelas/mapas
                isShiftF6Log = false;
                isConsoleLog = false;
                isErrorLog = false;

                logTableModel.setRowCount(0);
                callStackTableModel.setRowCount(0);
                shiftF6CallMap.clear();
                consoleLogCallMap.clear();

                if (foundConsoleLog) {
                    isConsoleLog = true;
                    setFileLabels("Tipo: LogProfiler (console.log)", "Arquivo: " + selectedFile.getName());
                    readConsoleLogFile(selectedFile);
                    tabbedPane.setSelectedIndex(0);

                } else if (foundShiftF6) {
                    isShiftF6Log = true;
                    setFileLabels("Tipo: LogProfiler (SHIFT+F6)", "Arquivo: " + selectedFile.getName());
                    readLogFile(selectedFile);
                    tabbedPane.setSelectedIndex(0);

                } else {
                    JOptionPane.showMessageDialog(this,
                            "Esse arquivo não parece ser um log do tipo Console ou SHIFT+F6.\nSe for um Error.log, abra pela aba correspondente.",
                            "Tipo inválido", JOptionPane.WARNING_MESSAGE);
                }

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Abre um arquivo na aba de Error Logs via JFileChooser.
     * Verifica se o arquivo contém "thread error" para confirmar ser um error.log.
     * Atualiza flags, limpa dados antigos e carrega os dados do arquivo.
     */
    private void handleErrorLogOpen(ActionEvent e) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // Look-and-feel nativo
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this); // Diálogo para selecionar arquivo

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            setTitle("Log Profiler View - " + selectedFile.getName()); // Atualiza título da janela

            try {
                List<String> linhas = Files.readAllLines(selectedFile.toPath(), StandardCharsets.ISO_8859_1); // Lê
                                                                                                              // linhas

                boolean isReallyError = false;
                for (String linha : linhas) {
                    if (linha.toLowerCase().contains("thread error")) {
                        isReallyError = true; // Confirma que é um error.log
                        break;
                    }
                }

                // Reset flags e limpa dados antigos
                isShiftF6Log = false;
                isConsoleLog = false;
                isErrorLog = false;

                logTableModel.setRowCount(0);
                callStackTableModel.setRowCount(0);
                shiftF6CallMap.clear();
                consoleLogCallMap.clear();

                if (isReallyError) {
                    isErrorLog = true;
                    setFileLabels("Tipo: Error.log", "Arquivo: " + selectedFile.getName());
                    readErrorLogFile(selectedFile);
                    tabbedPane.setSelectedIndex(1); // Seleciona aba Error Logs
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Esse arquivo não parece ser um Error.log.\nTente abrir pela aba de Logs comuns se for outro tipo.",
                            "Tipo inválido", JOptionPane.WARNING_MESSAGE);
                }

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Limpa a linha de log removendo prefixos de timestamp e thread,
     * extraindo apenas as partes importantes para análise:
     * - Trechos que começam com "CALL função(arquivo)"
     * - Trechos que começam com "-- FROM rotina(arquivo) [LN linha]"
     * Se não corresponder a esses padrões, retorna a linha sem espaços nas pontas.
     */
    private String cleanLine(String line) {
        // Regex para capturar prefixo timestamp + thread e trecho CALL(...)
        Pattern prefixPattern = Pattern.compile(
                "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}-\\d{2}:\\d{2}\\s+\\d+\\|\\[Thread\\s+\\d+\\]\\s+(CALL\\s+[\\w:.]+\\s*\\([^\\)]+\\))");
        Matcher matcher = prefixPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1); // Retorna só "CALL função(arquivo)"
        }

        // Regex para capturar prefixo timestamp + thread e trecho -- FROM ...
        Pattern fromPattern = Pattern.compile(
                "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}-\\d{2}:\\d{2}\\s+\\d+\\|\\[Thread\\s+\\d+\\]\\s+(-- FROM\\s+[\\w:.#]+\\s*\\([^\\)]+\\)\\s*(?:LN\\s+\\d+)?)");
        matcher = fromPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1); // Retorna só "-- FROM rotina(arquivo) [LN linha]"
        }

        // Caso não bata com nenhum padrão, só trim (remove espaços extras)
        return line.trim();
    }

    /**
     * Lê e processa arquivo de log gerado via SHIFT+F6.
     * Identifica o bloco principal do log, extrai chamadas e suas origens,
     * evita chamadas duplicadas e popula a tabela com os dados processados.
     *
     * @param file Arquivo de log a ser lido e processado
     */
    private void readLogFile(File file) {
        resetAllViews(); // Reseta visualizações e dados anteriores
        isShiftF6Log = true; // Marca que o arquivo atual é do tipo SHIFT+F6

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {

            StringBuilder content = new StringBuilder(); // Guarda conteúdo completo, se precisar
            List<String> allLinesList = new ArrayList<>(); // Guarda todas as linhas do arquivo
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n"); // Monta string com conteúdo completo (não usado diretamente)
                allLinesList.add(line); // Armazena cada linha para iteração posterior
            }

            String[] allLines = allLinesList.toArray(new String[0]); // Converte lista em array para facilitar indexação
            int beginIndex = -1;

            // Procura o índice do bloco principal do log, começando pelo fim do arquivo
            for (int i = allLines.length - 1; i >= 0; i--) {
                String raw = allLines[i];
                String clean = raw.trim();
                if (clean.startsWith("/* ========================================")) {
                    beginIndex = i;
                    break; // Encontrou o início do bloco principal, para busca
                }
            }

            // Se não encontrou o bloco principal, exibe erro e retorna
            if (beginIndex == -1) {
                tipoArquivoLabelLogs.setText("❌ Arquivo não é um LogProfiler válido (SHIFT+F6 não encontrado).");
                JOptionPane.showMessageDialog(this,
                        "Arquivo selecionado não contém um bloco válido de LogProfiler gerado via SHIFT+F6.\n"
                                + "Por favor, selecione um arquivo compatível.",
                        "Arquivo Inválido",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            shiftF6CallMap.clear(); // Limpa mapa de chamadas para armazenar novas
            int order = 1; // Contador de ordem para as chamadas
            Set<String> chamadasRegistradas = new HashSet<>(); // Evita duplicação de chamadas processadas
            List<String> currentBlock = new ArrayList<>(); // Armazena linhas do bloco atual de chamada

            // Percorre linhas a partir do início do bloco principal até o fim
            for (int i = beginIndex; i < allLines.length; i++) {
                String rawLine = allLines[i].trim();

                if (rawLine.isEmpty()) { // Linha vazia indica fim do bloco atual
                    if (!currentBlock.isEmpty()) {
                        String key = currentBlock.get(0).trim(); // Primeira linha do bloco (deve começar com CALL)
                        if (key.startsWith("CALL") && !chamadasRegistradas.contains(key)) {
                            processCallBlock(currentBlock, order++, false, shiftF6CallMap); // Processa bloco único
                            chamadasRegistradas.add(key); // Marca como já processado
                        }
                        currentBlock.clear();
                    }
                    continue;
                }

                if (rawLine.startsWith("CALL")) { // Nova chamada detectada
                    if (!currentBlock.isEmpty()) {
                        String key = currentBlock.get(0).trim();
                        if (key.startsWith("CALL") && !chamadasRegistradas.contains(key)) {
                            processCallBlock(currentBlock, order++, false, shiftF6CallMap);
                            chamadasRegistradas.add(key);
                        }
                        currentBlock.clear();
                    }
                    currentBlock.add(rawLine); // Inicia novo bloco de chamada
                } else if (rawLine.startsWith("-- FROM")) { // Linha de origem da chamada
                    currentBlock.add(rawLine); // Adiciona ao bloco atual
                }
            }

            // Processa último bloco, se existir
            if (!currentBlock.isEmpty() && currentBlock.get(0).startsWith("CALL")) {
                processCallBlock(currentBlock, order++, false, shiftF6CallMap);
            }

            showLogProfilerViews(); // Atualiza a interface para mostrar dados carregados

            // Seleciona a primeira linha da tabela, se houver dados
            if (logTableModel.getRowCount() > 0) {
                logTable.setRowSelectionInterval(0, 0);
            }

            tabbedPane.setSelectedIndex(0); // Vai para a aba Logs

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Atualiza a tabela de call stack exibindo as chamadas da função selecionada.
     * 
     * @param funcName Nome da função cuja call stack será exibida.
     */
    private void updateCallStack(String funcName) {
        if (funcName == null || funcName.trim().isEmpty()) {
            return; // Sai se nome inválido ou vazio
        }

        funcName = funcName.toUpperCase().trim(); // Normaliza para chave do mapa
        callStackTableModel.setRowCount(0); // Limpa tabela antes de preencher

        boolean isLogValido = isConsoleLog || isShiftF6Log;
        if (!isLogValido) {
            return; // Se não for um log válido, não faz nada
        }

        if (isShiftF6Log) {
            CallBlockInfo info = shiftF6CallMap.get(funcName); // Obtém info do mapa SHIFT+F6
            if (info != null && info.callStackEntries != null) {
                for (CallStackEntry entry : info.callStackEntries) {
                    callStackTableModel.addRow(new Object[] {
                            entry.getCalledFrom(),
                            entry.getSourceFile(),
                            entry.getLineNumber() > 0 ? entry.getLineNumber() : "N/A", // Linha ou "N/A"
                            entry.getNumCalls(),
                            String.format("%.3f", entry.getTotalTime()),
                            String.format("%.3f", entry.getMaxTime())
                    });
                }
            }
        } else if (isConsoleLog) {
            CallBlockInfo info = consoleLogCallMap.get(funcName); // Info do mapa console.log
            if (info != null) {
                List<CallStackEntry> entries = info.callStackEntries;
                if (entries != null) {
                    for (CallStackEntry entry : entries) {
                        callStackTableModel.addRow(new Object[] {
                                entry.getCalledFrom(),
                                entry.getSourceFile(),
                                entry.getLineNumber() > 0 ? entry.getLineNumber() : "N/A",
                                entry.getNumCalls(),
                                String.format("%.3f", entry.getTotalTime()),
                                String.format("%.3f", entry.getMaxTime())
                        });
                    }
                }
            }
        }
    }

    /**
     * Preenche a tabela de callstack a partir do mapa do SHIFT+F6.
     * Usado para logs do tipo SHIFT+F6, baseado na função selecionada.
     * 
     * @param selectedFunc Nome da função selecionada na tabela principal.
     * @param callMap      Mapa de informações de chamada (SHIFT+F6).
     */
    private void updateCallStackShiftF6(String selectedFunc, Map<String, CallBlockInfo> callMap) {
        callStackTableModel.setRowCount(0); // Limpa a tabela antes de popular

        if (selectedFunc == null || selectedFunc.isEmpty()) {
            return; // Sem função selecionada, não faz nada
        }

        selectedFunc = selectedFunc.toUpperCase().trim(); // Normaliza nome para maiúsculas e remove espaços

        CallBlockInfo info = callMap.get(selectedFunc); // Busca o bloco correspondente no mapa

        if (info != null && info.callStackEntries != null) {
            for (CallStackEntry entry : info.callStackEntries) {
                callStackTableModel.addRow(new Object[] {
                        entry.getCalledFrom(), // Quem chamou essa função
                        entry.getSourceFile(), // Arquivo fonte
                        entry.getLineNumber() > 0 ? entry.getLineNumber() : "N/A", // Linha ou "N/A"
                        entry.getNumCalls(), // Quantidade de chamadas (C)
                        String.format("%.3f", entry.getTotalTime()), // Tempo total (T)
                        String.format("%.3f", entry.getMaxTime()) // Tempo máximo (M)
                });
            }
        }
    }

    /**
     * Processa um bloco de chamadas (CALL + FROMs) e extrai informações para exibir
     * na tabela e no mapa.
     * 
     * @param blockLines Bloco contendo uma linha CALL e possíveis linhas FROM.
     * @param order      Ordem sequencial da chamada (para exibição).
     * @param isConsole  Define se o log é de console (true) ou SHIFT+F6 (false).
     * @param mapToUse   Mapa de destino para armazenar os dados processados.
     */
    private void processCallBlock(List<String> blockLines, int order, boolean isConsole,
            Map<String, CallBlockInfo> mapToUse) {
        if (blockLines.isEmpty())
            return; // Bloco vazio, sai fora

        String callLine = blockLines.get(0).trim(); // Primeira linha deve ser a CALL

        // Extrai função e fonte da linha CALL
        Pattern callPattern = Pattern.compile("CALL\\s+([^\\s]+)\\s*\\(([^)]+)\\)");
        Matcher callMatcher = callPattern.matcher(callLine);
        if (!callMatcher.find())
            return; // CALL mal formatado, ignora

        String funcName = callMatcher.group(1).trim(); // Nome da função
        String sourceFile = callMatcher.group(2).trim(); // Nome do fonte

        int numCallsCall = 0; // C x
        double maxTimeCall = 0.0; // M x.xxx
        double totalTimeCall = 0.0; // T x.xxx

        // Extrai C, T e M da linha CALL, se existirem
        try {
            Matcher matcherCalls = Pattern.compile("C\\s+(\\d+)").matcher(callLine);
            if (matcherCalls.find())
                numCallsCall = Integer.parseInt(matcherCalls.group(1));

            Matcher matcherTotalTime = Pattern.compile("T\\s+([\\d\\.]+)").matcher(callLine);
            if (matcherTotalTime.find())
                totalTimeCall = Double.parseDouble(matcherTotalTime.group(1));

            Matcher matcherMaxTime = Pattern.compile("M\\s+([\\d\\.]+)").matcher(callLine);
            if (matcherMaxTime.find())
                maxTimeCall = Double.parseDouble(matcherMaxTime.group(1));
        } catch (Exception e) {
            e.printStackTrace(); // Não bloqueia o processo se der erro na leitura de C/T/M
        }

        // Adiciona linha na tabela principal
        logTableModel.addRow(new Object[] {
                String.format("# %06d", order), // Número da ordem (com zeros à esquerda)
                funcName.toUpperCase(), // Nome da função (uppercase)
                sourceFile, // Nome do fonte
                numCallsCall, // Chamadas (C)
                maxTimeCall // Tempo máximo (M)
        });

        List<CallStackEntry> callStackList = new ArrayList<>(); // Lista com os FROMs da função

        for (int i = 1; i < blockLines.size(); i++) {
            String fromLine = blockLines.get(i).trim();

            // Expressão para capturar os dados da linha "-- FROM"
            Pattern fromPattern = Pattern.compile(
                    "-- FROM\\s+([^\\s]+)\\s*\\(([^)]+)\\)(?:\\s*\\((\\d+)\\)|\\s*LN\\s+(\\d+))?" +
                            "(?:\\s*C\\s+(\\d+))?" +
                            "(?:\\s*T\\s+([\\d\\.]+))?" +
                            "(?:\\s*M\\s+([\\d\\.]+))?");
            Matcher fromMatcher = fromPattern.matcher(fromLine);

            // Valores padrão
            String calledFrom = "-";
            String fromFile = "-";
            int fromLineNumber = -1;
            int numCallsFrom = 0;
            double totalTimeFrom = 0.0;
            double maxTimeFrom = 0.0;

            if (fromMatcher.find()) {
                calledFrom = fromMatcher.group(1).trim(); // Rotina chamada
                fromFile = fromMatcher.group(2).trim(); // Fonte

                // Linha: pode vir como (123) ou LN 123
                String lineGroup = fromMatcher.group(3) != null ? fromMatcher.group(3) : fromMatcher.group(4);
                if (lineGroup != null) {
                    try {
                        fromLineNumber = Integer.parseInt(lineGroup.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }

                // C, T e M opcionais
                try {
                    if (fromMatcher.group(5) != null)
                        numCallsFrom = Integer.parseInt(fromMatcher.group(5));
                } catch (NumberFormatException ignored) {
                }
                try {
                    if (fromMatcher.group(6) != null)
                        totalTimeFrom = Double.parseDouble(fromMatcher.group(6));
                } catch (NumberFormatException ignored) {
                }
                try {
                    if (fromMatcher.group(7) != null)
                        maxTimeFrom = Double.parseDouble(fromMatcher.group(7));
                } catch (NumberFormatException ignored) {
                }
            }

            // Cria objeto para a entrada do callstack
            CallStackEntry entry = new CallStackEntry(calledFrom, fromFile, fromLineNumber, numCallsFrom, totalTimeFrom,
                    maxTimeFrom);
            callStackList.add(entry); // Adiciona à lista da função

            // Também adiciona à lista geral de chamadas (para exportar futuramente, etc.)
            fullCallStackRows.add(new Vector<>(Arrays.asList(
                    calledFrom,
                    fromFile,
                    String.valueOf(fromLineNumber),
                    String.valueOf(numCallsFrom),
                    String.format("%.3f", totalTimeFrom),
                    String.format("%.3f", maxTimeFrom))));
        }

        String key = funcName.toUpperCase().trim(); // Chave do mapa
        CallBlockInfo info = new CallBlockInfo(funcName, sourceFile, numCallsCall, maxTimeCall, callStackList);
        mapToUse.put(key, info); // Salva o resultado no mapa apropriado
    }

    /**
     * Representa um bloco de chamada (CALL + FROMs) com estatísticas e detalhes do
     * callstack.
     */
    private static class CallBlockInfo {
        String funcName; // Nome da função chamada
        String sourceFile; // Fonte onde a função está
        int numCalls; // Quantidade de chamadas (C)
        double maxTimeMs; // Tempo máximo (M)
        List<CallStackEntry> callStackEntries; // Lista de chamadas internas (FROMs)

        public CallBlockInfo(String funcName, String sourceFile, int numCalls, double maxTimeMs,
                List<CallStackEntry> callStackEntries) {
            this.funcName = funcName;
            this.sourceFile = sourceFile;
            this.numCalls = numCalls;
            this.maxTimeMs = maxTimeMs;
            this.callStackEntries = callStackEntries;
        }
    }

    /**
     * Realiza a leitura e interpretação de um arquivo error.log.
     * Extrai erro, ambiente, query SQL e callstack, se houver.
     */
    private void readErrorLogFile(File file) throws IOException {

        resetAllViews(); // Limpa todas as visualizações anteriores

        List<String> lines;
        try {
            // Tenta ler o arquivo como UTF-8
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (MalformedInputException ex) {
            // Se falhar, tenta ISO-8859-1
            lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        }

        // Se for um console.log, redireciona para o parser correto
        boolean isConsoleLog = lines.stream().anyMatch(l -> l.contains("--- BEGIN APP PROFILER"));
        if (isConsoleLog) {
            readConsoleLogFile(file);
            return;
        }

        tipoArquivoLabelErros.setText("✔️ Error.log detectado");
        envTableModel.setRowCount(0); // Limpa tabela de ambiente
        errorPromptArea.setText(""); // Limpa mensagem de erro
        queryTextArea.setText(""); // Limpa query SQL

        extrairMensagemErroPrompt(lines); // Extrai mensagem de erro principal
        parseEnvironmentBlock(lines); // Extrai informações do ambiente

        // Captura a query SQL (SELECT, INSERT, etc.)
        StringBuilder queryBuilder = new StringBuilder(); // Acumula as linhas da query
        boolean insideQuery = false; // Flag para saber se estamos dentro de uma query

        for (int i = 0; i < lines.size(); i++) {
            String linha = lines.get(i);
            String trimmedUpper = linha.trim().toUpperCase();

            // Detecta início da query
            if (trimmedUpper.startsWith("SELECT") || trimmedUpper.startsWith("INSERT") ||
                    trimmedUpper.startsWith("UPDATE") || trimmedUpper.startsWith("DELETE") ||
                    trimmedUpper.startsWith("WITH")) {
                insideQuery = true;
            }

            // Acumula as linhas enquanto a query não acabar
            if (insideQuery) {
                if (linha.trim().isEmpty()) {
                    insideQuery = false;
                } else {
                    queryBuilder.append(linha).append("\n");
                }
            }
        }

        String query = queryBuilder.toString().trim(); // Converte para string final

        // Exibe no painel ou mensagem padrão
        if (query.isEmpty()) {
            queryTextArea.setText("Nenhuma query SQL encontrada neste log.");
        } else {
            queryTextArea.setText(query);
        }
        queryTextArea.setCaretPosition(0); // Volta para o topo

        // Coleta e exibe o bloco de callstack (se houver)
        List<String> callStackLines = new ArrayList<>();
        boolean inCallStack = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("Called from")) {
                inCallStack = true;
            }

            if (inCallStack) {
                if (trimmed.isEmpty()) {
                    break; // fim do bloco
                }
                callStackLines.add(line);
            }
        }

        if (callStackLines.isEmpty()) {
            // Sem callstack: mostra aviso e ajusta título do painel
            JOptionPane.showMessageDialog(this,
                    "O arquivo error.log aberto não possui callstack para exibir.",
                    "Atenção",
                    JOptionPane.INFORMATION_MESSAGE);

            middleTablePanel.setBorder(BorderFactory.createTitledBorder("Arquivo não possui callstack"));
            middleTablePanel.repaint();

        } else {
            // Com callstack: processa e exibe
            parseErrorCallStack(callStackLines);
            middleTablePanel.setBorder(BorderFactory.createTitledBorder("Callstack"));
            middleTablePanel.repaint();
        }

        tabbedPane.setSelectedIndex(1); // Vai pra aba de error log
    }

    /**
     * Extrai a mensagem principal de erro do bloco THREAD ERROR em um error.log
     * e exibe no painel de erro formatado.
     */
    private void extrairMensagemErroPrompt(List<String> lines) {
        StringBuilder blocoErro = new StringBuilder(); // Acumulador do bloco de erro
        boolean capturando = false; // Flag que indica se estamos dentro do bloco de erro

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (capturando) {
                    break; // Fim do bloco de erro
                } else {
                    continue; // Pula linhas em branco
                }
            }

            if (!capturando && line.trim().startsWith("THREAD ERROR")) {
                capturando = true;
                blocoErro.append(line).append("\n");
                continue;
            }

            if (capturando) {
                blocoErro.append(line).append("\n"); // Continua capturando o bloco
            }
        }

        String[] blocos = blocoErro.toString().split("\n"); // Separa linhas do bloco
        if (blocos.length >= 2) {
            String linhaErro = blocos[1]; // A 2ª linha normalmente contém o erro detalhado

            // Expressão para extrair mensagem, rotina, fonte, data/hora e linha
            Pattern pattern = Pattern.compile(
                    "(.*) on (\\w+)\\(([^)]+)\\) (\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}) line ?: (\\d+)",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(linhaErro);
            if (matcher.find()) {
                String mensagem = matcher.group(1).trim(); // Mensagem de erro
                String rotina = matcher.group(2).trim(); // Nome da rotina
                String fonte = matcher.group(3).trim(); // Nome do fonte .prw
                String data = matcher.group(4).trim(); // Data e hora
                String linha = matcher.group(5).trim(); // Linha do erro

                // Monta visualização formatada no painel
                StringBuilder prompt = new StringBuilder();
                prompt.append("Erro: ").append(mensagem).append("\n");
                prompt.append("Rotina: ").append(rotina).append("\n");
                prompt.append("Fonte: ").append(fonte).append("\n");
                prompt.append("Data/Hora: ").append(data).append("\n");
                prompt.append("Linha: ").append(linha);

                errorPromptArea.setText(prompt.toString()); // Mostra erro formatado
                errorPromptArea.setCaretPosition(0); // Scroll pro topo
            } else {
                // Fallback: se regex falhar, mostra o bloco bruto
                errorPromptArea.setText(blocoErro.toString());
            }
        }
    }

    /**
     * Extrai e exibe as informações de ambiente (TOTVS Environment)
     * em uma tabela, a partir de um error.log.
     */
    private void parseEnvironmentBlock(List<String> lines) {
        envTableModel.setRowCount(0); // Limpa a tabela de ambiente

        boolean lendoAmbiente = false; // Flag que ativa a leitura do bloco
        for (String line : lines) {
            line = line.trim(); // Remove espaços laterais

            if (line.startsWith("[TOTVS Environment") || line.startsWith("[TOTVS ") || lendoAmbiente) {
                lendoAmbiente = true;

                if (line.startsWith("[") && line.endsWith("]")) {
                    String conteudo = line.substring(1, line.length() - 1); // Remove os colchetes
                    String campo, valor; // Campo e valor que serão exibidos

                    if (conteudo.contains(":")) {
                        String[] partes = conteudo.split(": ", 2); // Separa em "Campo: Valor"
                        campo = partes[0].trim(); // Ex: TOTVS Environment
                        valor = partes[1].trim(); // Ex: Desenvolvimento P12
                    } else {
                        campo = conteudo.trim(); // Ex: apenas "[TOTVS Environment]" sem valor
                        valor = "";
                    }

                    envTableModel.addRow(new Object[] { campo, valor }); // Adiciona à tabela
                }
            }
        }

        envTableModel.fireTableDataChanged(); // Notifica modelo da mudança
        envTable.revalidate(); // Revalida layout
        envTable.repaint(); // Força redesenho
    }

    /**
     * Faz o parsing da callstack encontrada em arquivos error.log
     * e popula a tabela central com origem, fonte, data/hora e linha.
     */
    private void parseErrorCallStack(List<String> lines) {
        middleTableModel.setRowCount(0); // Limpa a tabela antes de preencher

        // Regex para chamadas com bloco {|| ... } contendo fonte, data, hora e linha
        Pattern patternBloco = Pattern.compile(
                "Called from (\\{\\|\\|.*?\\})\\((.+?)\\)\\s+(\\d{2}/\\d{2}/\\d{4}) (\\d{2}:\\d{2}:\\d{2}) line ?: (\\d+)",
                Pattern.CASE_INSENSITIVE);

        // Regex padrão completo: rotina(fonte) data hora linha
        Pattern patternCompleto = Pattern.compile(
                "Called from (.+?)\\((.+?)\\)\\s+(\\d{2}/\\d{2}/\\d{4}) (\\d{2}:\\d{2}:\\d{2}) line ?: (\\d+)",
                Pattern.CASE_INSENSITIVE);

        // Regex mais simples: apenas ::ROTINA (sem info extra)
        Pattern patternSimples = Pattern.compile(
                "Called from (::.+)",
                Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            line = line.trim(); // Remove espaços laterais
            if (!line.startsWith("Called from"))
                continue; // Ignora irrelevantes

            Vector<String> row = new Vector<>(); // Linha que será adicionada na tabela

            Matcher matcherBloco = patternBloco.matcher(line); // Bloco {|| ... }
            Matcher matcherCompleto = patternCompleto.matcher(line); // Rotina completa
            Matcher matcherSimples = patternSimples.matcher(line); // Apenas rotina simples

            if (matcherBloco.find()) {
                String calledFrom = matcherBloco.group(1).trim(); // Ex: {|| bloco }
                String rotinaFonte = matcherBloco.group(2).trim(); // Ex: NOME.PRW
                String data = matcherBloco.group(3).trim(); // Ex: 01/01/2025
                String hora = matcherBloco.group(4).trim(); // Ex: 12:34:56
                String linha = matcherBloco.group(5).trim(); // Ex: 128

                row.add(calledFrom); // Called From
                row.add(rotinaFonte); // Fonte
                row.add(data + " " + hora); // Data + Hora
                row.add(linha); // Linha

            } else if (matcherCompleto.find()) {
                String calledFrom = matcherCompleto.group(1).trim(); // Ex: U_Func
                String rotinaFonte = matcherCompleto.group(2).trim(); // Ex: MAIN.PRW
                String data = matcherCompleto.group(3).trim();
                String hora = matcherCompleto.group(4).trim();
                String linha = matcherCompleto.group(5).trim();

                row.add(calledFrom);
                row.add(rotinaFonte);
                row.add(data + " " + hora);
                row.add(linha);

            } else if (matcherSimples.find()) {
                String calledFrom = matcherSimples.group(1).trim(); // Ex: ::Rotina

                row.add(calledFrom);
                row.add(""); // Sem fonte
                row.add(""); // Sem data/hora
                row.add(""); // Sem linha
            }

            if (!row.isEmpty()) {
                middleTableModel.addRow(row); // Adiciona a linha na tabela
            }
        }
    }

    /**
     * Atualiza o painel principal com a visualização das tabelas de logs
     * e callstack (modo SHIFT+F6 ou console.log).
     */
    private void showLogProfilerViews() {
        Component centerComp = getContentPane().getComponent(1); // Painel central da janela (esperado: JPanel)

        if (centerComp instanceof JPanel) {
            JPanel panel = (JPanel) centerComp;

            panel.removeAll(); // Remove qualquer conteúdo anterior
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Define layout vertical (empilha)

            panel.add(new JScrollPane(logTable)); // Adiciona tabela de logs com barra de rolagem
            panel.add(new JScrollPane(callStackTable)); // Adiciona tabela de callstack com barra de rolagem

            panel.revalidate(); // Atualiza layout
            panel.repaint(); // Reforça a renderização
        }
    }

    /**
     * Aplica o tema claro à interface, ajustando cores de fundo, texto e seleção
     * para tabelas, labels, abas e cabeçalhos.
     */
    private void applyTheme() {
        // 🎨 Definição das cores base do tema claro
        Color bgColor = Color.WHITE; // Fundo geral
        Color fgColor = Color.BLACK; // Cor do texto
        Color tableBg = Color.WHITE; // Fundo das tabelas
        Color tableFg = fgColor; // Texto das tabelas
        Color selectionBg = new Color(184, 207, 229); // Azul claro para seleção

        // 🧱 Aplica nas áreas principais
        getContentPane().setBackground(bgColor); // Fundo da janela principal
        tipoArquivoLabelLogs.setForeground(Color.BLUE); // Destaque para tipo de log
        footerLabel.setForeground(fgColor); // Rodapé

        // 📋 Tabela principal de logs
        logTable.setBackground(tableBg);
        logTable.setForeground(tableFg);
        logTable.setSelectionBackground(selectionBg);
        logTable.setSelectionForeground(tableFg);

        // 🧭 Tabela da pilha de chamadas
        callStackTable.setBackground(tableBg);
        callStackTable.setForeground(tableFg);
        callStackTable.setSelectionBackground(selectionBg);
        callStackTable.setSelectionForeground(tableFg);

        // 🎯 Renderer customizado para alinhamento e cor nas colunas da callstack
        CallStackRenderer callStackRenderer = new CallStackRenderer(selectionBg, tableFg, tableFg);
        callStackTable.getColumnModel().getColumn(0).setCellRenderer(callStackRenderer); // Called From
        callStackTable.getColumnModel().getColumn(1).setCellRenderer(callStackRenderer); // Source File
        callStackTable.getColumnModel().getColumn(2).setCellRenderer(callStackRenderer); // Line Number

        callStackTable.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Fonte fixa
        callStackTable.setRowHeight(24); // Altura das linhas

        // 🌍 Tabela de ambiente
        envTable.setBackground(tableBg);
        envTable.setForeground(tableFg);
        envTable.setSelectionBackground(selectionBg);
        envTable.setSelectionForeground(tableFg);

        // 🧭 Aplica tema no painel superior (barra de opções)
        Component headerPanel = ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (headerPanel instanceof JPanel) {
            for (Component c : ((JPanel) headerPanel).getComponents()) {
                updateComponentTheme(c, bgColor, fgColor); // Aplica nas labels, botões, etc.
                if (c instanceof JPanel) {
                    for (Component cc : ((JPanel) c).getComponents()) {
                        updateComponentTheme(cc, bgColor, fgColor); // Aplica em painéis aninhados
                    }
                }
            }
        }

        // 🏷️ Estilo dos cabeçalhos das tabelas
        JTableHeader[] headers = {
                logTable.getTableHeader(),
                callStackTable.getTableHeader(),
                envTable.getTableHeader()
        };

        for (JTableHeader header : headers) {
            header.setBackground(new Color(240, 240, 240)); // Cinza claro padrão de cabeçalho
            header.setForeground(Color.BLACK); // Texto preto
            header.setFont(new Font("SansSerif", Font.BOLD, 12)); // Negrito e limpo
        }

        // 📑 Abas principais
        tabbedPane.setBackground(bgColor);
        tabbedPane.setForeground(fgColor);

        repaint(); // 🔄 Força redesenho com as novas cores
    }

    /**
     * Atualiza o tema visual de um componente e seus filhos (se houver),
     * aplicando cores de fundo e texto de forma coerente com o tema ativo.
     */
    private void updateComponentTheme(Component c, Color bg, Color fg) {
        if (c instanceof JPanel) {
            // 🧱 Painel: aplica fundo e percorre os filhos recursivamente
            c.setBackground(bg);
            for (Component cc : ((JPanel) c).getComponents()) {
                updateComponentTheme(cc, bg, fg);
            }

        } else if (c instanceof JLabel) {
            // 🏷️ Label: altera cor do texto e fundo
            c.setForeground(fg);
            c.setBackground(bg);

        } else if (c instanceof JButton) {
            // 🔘 Botão: mantém cor de texto padrão e aplica fundo suave
            c.setForeground(fg);
            c.setBackground(new Color(245, 245, 245)); // Cinza claro, estilo Windows

        } else if (c instanceof JTextField) {
            // ✏️ Campo de texto: aplica texto, fundo branco e cor da barra de digitação
            c.setForeground(fg);
            c.setBackground(Color.WHITE);
            ((JTextField) c).setCaretColor(fg); // Cor do cursor
        }
    }

    // Reseta tudo: limpa tabelas, prompt, flags e força repaint pra evitar bug
    // visual
    private void resetAllViews() {
        logTableModel.setRowCount(0); // Limpa tabela de logs
        callStackTableModel.setRowCount(0); // Limpa tabela da callstack
        envTableModel.setRowCount(0); // Limpa tabela de ambiente
        middleTableModel.setRowCount(0); // Limpa tabela do meio (error callstack)

        errorPromptArea.setText(""); // Limpa área de mensagem de erro

        isErrorLog = false; // Reseta flag de Error.log
        isConsoleLog = false; // Reseta flag de LogProfiler console
        isShiftF6Log = false; // Reseta flag de LogProfiler SHIFT+F6

        // Garante atualização visual das tabelas (em caso de travamentos)
        logTable.repaint();
        callStackTable.repaint();
        envTable.repaint();
        middleTable.repaint();
    }

    public static void main(String[] args) {
        // Garante que a GUI seja criada na thread certa do Swing (Event Dispatch
        // Thread)
        SwingUtilities.invokeLater(() -> new LogProfilerView().setVisible(true));
    }

}
