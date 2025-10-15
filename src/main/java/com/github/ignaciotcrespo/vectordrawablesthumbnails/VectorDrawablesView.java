package com.github.ignaciotcrespo.vectordrawablesthumbnails;

import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class VectorDrawablesView {
    private JPanel panelMain;
    private JScrollPane vectorsContainer;
    private JPanel panelVectors;
    private JButton btnRefresh;
    private JTextField textFilter;
    private JButton clearButton;
    private JPanel panelFilter;
    private JButton btnDonate;
    private JComboBox comboSort;
    private JComboBox comboSortDirection;
    
    // Enhanced filtering components
    private JComboBox comboComplexityFilter;
    private JComboBox comboUsageFilter;
    private JSlider sliderFileSizeMax;
    private JSlider sliderColorCount;
    private JTextField textTagsFilter;
    private JCheckBox checkShowAnimated;
    private JCheckBox checkShowOptimizable;
    private JButton btnResetFilters;
    private JButton btnPresetUnused;
    private JButton btnPresetComplex;
    private JButton btnPresetOptimizable;
    private JLabel labelResultCount;
    private com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.ColorFilterPanel colorFilterPanel;
    private JCheckBox checkIncludeVectorDrawable;
    private JCheckBox checkIncludeSvg;

    public VectorDrawablesView() {
//        System.out.println("VectorDrawablesView: Constructor called");
        initializeComponents();
//        System.out.println("VectorDrawablesView: Constructor completed, panelMain = " + panelMain);
    }

    private void initializeComponents() {
//        System.out.println("VectorDrawablesView: initializeComponents called");
        if (panelMain == null) {
//            System.out.println("VectorDrawablesView: Creating UI components...");
            createUIComponents();
//            System.out.println("VectorDrawablesView: UI components created");
        }
        
        // Initialize combo boxes with default values
        if (comboSort != null) {
            comboSort.setSelectedItem("By Name");
//            System.out.println("VectorDrawablesView: comboSort initialized");
        }
        if (comboSortDirection != null) {
            comboSortDirection.setSelectedItem("Asc");
//            System.out.println("VectorDrawablesView: comboSortDirection initialized");
        }
        if (comboComplexityFilter != null) {
            comboComplexityFilter.setSelectedItem("All");
//            System.out.println("VectorDrawablesView: comboComplexityFilter initialized");
        }
        if (comboUsageFilter != null) {
            comboUsageFilter.setSelectedItem("All");
//            System.out.println("VectorDrawablesView: comboUsageFilter initialized");
        }
//        System.out.println("VectorDrawablesView: initializeComponents completed");
    }

    public JButton getBtnRefresh() {
        return btnRefresh;
    }

    public JPanel getContent() {
        return panelMain;
    }

    public JPanel getPanelVectors() {
        return panelVectors;
    }

    public JTextField getTextFilter() {
        return textFilter;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public JPanel getPanelFilter() {
        return panelFilter;
    }

    public JButton getBtnDonate() {
        return btnDonate;
    }

    public JComboBox getComboSort() {
        return comboSort;
    }

    public JComboBox getComboSortDirection() {
        return comboSortDirection;
    }

    public JComboBox getComboComplexityFilter() {
        return comboComplexityFilter;
    }

    public JComboBox getComboUsageFilter() {
        return comboUsageFilter;
    }

    public JSlider getSliderFileSizeMax() {
        return sliderFileSizeMax;
    }
    
    public JSlider getSliderColorCount() {
        return sliderColorCount;
    }

    public JTextField getTextTagsFilter() {
        return textTagsFilter;
    }

    public JCheckBox getCheckShowAnimated() {
        return checkShowAnimated;
    }

    public JCheckBox getCheckShowOptimizable() {
        return checkShowOptimizable;
    }

    public JButton getBtnResetFilters() {
        return btnResetFilters;
    }

    public JButton getBtnPresetUnused() {
        return btnPresetUnused;
    }

    public JButton getBtnPresetComplex() {
        return btnPresetComplex;
    }

    public JButton getBtnPresetOptimizable() {
        return btnPresetOptimizable;
    }

    public JLabel getLabelResultCount() {
        return labelResultCount;
    }

    public com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.ColorFilterPanel getColorFilterPanel() {
        return colorFilterPanel;
    }

    public JCheckBox getCheckIncludeVectorDrawable() {
        return checkIncludeVectorDrawable;
    }

    public JCheckBox getCheckIncludeSvg() {
        return checkIncludeSvg;
    }

    private void createUIComponents() {
        panelMain = new JPanel();
        panelMain.setLayout(new BorderLayout());

        // Create enhanced filter panel
        panelFilter = createEnhancedFilterPanel();
        
        // Create buttons panel
        JPanel buttonPanel = createButtonPanel();

        // Create file type selection panel
        JPanel fileTypePanel = createFileTypeSelectionPanel();

        // Create north panel with better organization
        JPanel northPanel = new JPanel(new BorderLayout());
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(buttonPanel, BorderLayout.NORTH);
        topSection.add(fileTypePanel, BorderLayout.CENTER);
        northPanel.add(topSection, BorderLayout.NORTH);
        northPanel.add(panelFilter, BorderLayout.CENTER);
        
        panelMain.add(northPanel, BorderLayout.NORTH);

        // Create vectors panel
        panelVectors = new JPanel();
        GridLayout gridLayout = new GridLayout(0, 3);
        gridLayout.setVgap(20);
        panelVectors.setLayout(gridLayout);

        vectorsContainer = new JBScrollPane(panelVectors);
        vectorsContainer.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        vectorsContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        panelMain.add(vectorsContainer, BorderLayout.CENTER);
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());

        // Left side - refresh and result count
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRefresh = new JButton("🔄 Refresh");
        labelResultCount = new JLabel("0 vectors");
        labelResultCount.setForeground(Color.GRAY);
        leftPanel.add(btnRefresh);
        leftPanel.add(Box.createHorizontalStrut(10));
        leftPanel.add(labelResultCount);

        // Right side - donate button
        btnDonate = new JButton("♡ Support");
        btnDonate.setToolTipText("Support the development of this plugin");

        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(btnDonate, BorderLayout.EAST);

        return buttonPanel;
    }

    private JPanel createFileTypeSelectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("📁 File Types"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        checkIncludeVectorDrawable = new JCheckBox("Vector Drawables (.xml)");
        checkIncludeVectorDrawable.setSelected(true); // Enabled by default
        checkIncludeVectorDrawable.setToolTipText("Include Android Vector Drawable XML files");

        checkIncludeSvg = new JCheckBox("SVG files (.svg)");
        checkIncludeSvg.setSelected(true);
        checkIncludeSvg.setToolTipText("Include SVG files");

        panel.add(checkIncludeVectorDrawable);
        panel.add(Box.createHorizontalStrut(15));
        panel.add(checkIncludeSvg);

        return panel;
    }
    
    private JPanel createEnhancedFilterPanel() {
//        System.out.println("VectorDrawablesView: Creating enhanced filter panel with tabs...");
        JPanel mainFilterPanel = new JPanel(new BorderLayout());
        mainFilterPanel.setBorder(BorderFactory.createTitledBorder("🔍 Advanced Filters & Sorting"));
        
        // Create tabbed pane for better organization
        JTabbedPane tabbedPane = new JTabbedPane();
//        System.out.println("VectorDrawablesView: Created JTabbedPane");
        
        // Basic filters tab
        JPanel basicPanel = createBasicFiltersPanel();
        tabbedPane.addTab("Basic", basicPanel);
//        System.out.println("VectorDrawablesView: Added Basic tab");
        
        // Advanced filters tab
        JPanel advancedPanel = createAdvancedFiltersPanel();
        tabbedPane.addTab("Advanced", advancedPanel);
//        System.out.println("VectorDrawablesView: Added Advanced tab");
        
        // Presets tab
        JPanel presetsPanel = createPresetsPanel();
        tabbedPane.addTab("Presets", presetsPanel);
//        System.out.println("VectorDrawablesView: Added Presets tab");
        
        // Colors tab with both color filter and color count slider
        JPanel colorsTabPanel = createColorsTabPanel();
        tabbedPane.addTab("Colors", colorsTabPanel);
        
        mainFilterPanel.add(tabbedPane, BorderLayout.CENTER);
//        System.out.println("VectorDrawablesView: Enhanced filter panel created with " + tabbedPane.getTabCount() + " tabs");
        
        return mainFilterPanel;
    }
    
    private JPanel createBasicFiltersPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Text filter row
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Search:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        textFilter = new JTextField();
        textFilter.setToolTipText("Search by name, tags, or description");
        panel.add(textFilter, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        clearButton = new JButton("Clear");
        panel.add(clearButton, gbc);

        // Sort row (file type checkboxes moved to top panel)
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Sort By:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridwidth = 1;
        comboSort = new JComboBox<>(new String[]{
            "By Name", "By Width", "By Height", "By Width x Height",
            "By File Size", "By Complexity", "By Usage Count", "By Tags"
        });
        panel.add(comboSort, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.gridwidth = 1;
        comboSortDirection = new JComboBox<>(new String[]{"Asc", "Desc"});
        panel.add(comboSortDirection, gbc);
        
        return panel;
    }
    
    private JPanel createAdvancedFiltersPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Complexity filter
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Complexity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        comboComplexityFilter = new JComboBox<>(new String[]{
            "All", "Simple", "Moderate", "Complex", "Very Complex"
        });
        panel.add(comboComplexityFilter, gbc);
        
        // Usage filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Usage:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        comboUsageFilter = new JComboBox<>(new String[]{
            "All", "Unused", "Rarely Used", "Used", "Frequently Used"
        });
        panel.add(comboUsageFilter, gbc);
        
        // File size filter with improved layout
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Max File Size:"), gbc);
        
        // Create a panel for slider and its label
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel sliderPanel = new JPanel(new BorderLayout());
        
        sliderFileSizeMax = new JSlider(0, 50, 50); // 0-50KB
        sliderFileSizeMax.setMajorTickSpacing(10);
        sliderFileSizeMax.setMinorTickSpacing(5);
        sliderFileSizeMax.setPaintTicks(true);
        sliderFileSizeMax.setPaintLabels(true);
        sliderFileSizeMax.setToolTipText("Maximum file size in KB");
        
        // Add value label for immediate feedback
        JLabel sliderValueLabel = new JLabel("No limit");
        sliderValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sliderValueLabel.setFont(sliderValueLabel.getFont().deriveFont(Font.BOLD));
        
        // Update label when slider changes
        sliderFileSizeMax.addChangeListener(e -> {
            JSlider slider = (JSlider) e.getSource();
            int value = slider.getValue();
            if (value >= 50) {
                sliderValueLabel.setText("No limit");
            } else {
                sliderValueLabel.setText(value + " KB");
            }
        });
        
        sliderPanel.add(sliderFileSizeMax, BorderLayout.CENTER);
        sliderPanel.add(sliderValueLabel, BorderLayout.SOUTH);
        panel.add(sliderPanel, gbc);
        
        // Tags filter
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Tags:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        textTagsFilter = new JTextField();
        textTagsFilter.setToolTipText("Filter by tags (comma-separated)");
        panel.add(textTagsFilter, gbc);
        
        // Checkboxes
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        checkShowAnimated = new JCheckBox("Show only animated vectors");
        panel.add(checkShowAnimated, gbc);
        
        gbc.gridy = 5;
        checkShowOptimizable = new JCheckBox("Show only vectors with optimization suggestions");
        panel.add(checkShowOptimizable, gbc);
        
        // Reset button
        gbc.gridy = 6; gbc.gridwidth = 1; gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST;
        btnResetFilters = new JButton("🔄 Reset All Filters");
        panel.add(btnResetFilters, gbc);
        
        return panel;
    }
    
    private JPanel createPresetsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Preset buttons with descriptions
        gbc.gridy = 0;
        btnPresetUnused = new JButton("🚫 Show Unused Vectors");
        btnPresetUnused.setToolTipText("Find vectors that are not used in your project");
        panel.add(btnPresetUnused, gbc);
        
        gbc.gridy = 1;
        btnPresetComplex = new JButton("⚠️ Show Complex Vectors");
        btnPresetComplex.setToolTipText("Find vectors with high complexity that might need optimization");
        panel.add(btnPresetComplex, gbc);
        
        gbc.gridy = 2;
        btnPresetOptimizable = new JButton("🔧 Show Optimizable Vectors");
        btnPresetOptimizable.setToolTipText("Find vectors with optimization suggestions");
        panel.add(btnPresetOptimizable, gbc);
        
        // Add descriptions
        gbc.gridy = 3; gbc.insets = new Insets(20, 10, 10, 10);
        JLabel descLabel = new JLabel("<html><b>Quick Presets:</b><br/>" +
            "• <b>Unused:</b> Vectors not referenced in layout files<br/>" +
            "• <b>Complex:</b> Vectors with high complexity scores<br/>" +
            "• <b>Optimizable:</b> Vectors with optimization opportunities</html>");
        descLabel.setForeground(Color.GRAY);
        panel.add(descLabel, gbc);
        
        return panel;
    }
    
    private JPanel createColorsTabPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Add color count slider at the top
        JPanel colorCountPanel = new JPanel(new BorderLayout());
        colorCountPanel.setBorder(BorderFactory.createTitledBorder("Filter by Number of Colors"));
        
        sliderColorCount = new JSlider(-1, 10, -1); // -1: all, 0-10 colors
        sliderColorCount.setMajorTickSpacing(1);
        sliderColorCount.setPaintTicks(true);
        sliderColorCount.setPaintLabels(true);
        
        // Create custom labels for the slider
        java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
        labelTable.put(-1, new JLabel("All"));
        for (int i = 0; i <= 10; i++) {
            labelTable.put(i, new JLabel(String.valueOf(i)));
        }
        sliderColorCount.setLabelTable(labelTable);
        sliderColorCount.setToolTipText("All: no filter, 0: no colors, 1-9: exactly that many colors, 10: 10 or more colors");
        
        // Add value label for immediate feedback
        JLabel colorCountLabel = new JLabel("All (no filter)");
        colorCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        colorCountLabel.setFont(colorCountLabel.getFont().deriveFont(Font.BOLD));
        
        // Update label when slider changes
        sliderColorCount.addChangeListener(e -> {
            JSlider slider = (JSlider) e.getSource();
            int value = slider.getValue();
            if (value == -1) {
                colorCountLabel.setText("All (no filter)");
            } else if (value == 0) {
                colorCountLabel.setText("Exactly 0 colors");
            } else if (value == 1) {
                colorCountLabel.setText("Exactly 1 color");
            } else if (value < 10) {
                colorCountLabel.setText("Exactly " + value + " colors");
            } else {
                colorCountLabel.setText("10 or more colors");
            }
        });
        
        colorCountPanel.add(sliderColorCount, BorderLayout.CENTER);
        colorCountPanel.add(colorCountLabel, BorderLayout.SOUTH);
        
        // Add color filter panel below
        colorFilterPanel = new com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.ColorFilterPanel();
        
        panel.add(colorCountPanel, BorderLayout.NORTH);
        panel.add(colorFilterPanel, BorderLayout.CENTER);
        
        return panel;
    }
}
