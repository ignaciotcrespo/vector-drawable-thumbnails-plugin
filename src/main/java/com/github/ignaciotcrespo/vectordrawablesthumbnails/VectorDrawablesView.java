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
    private JTextField textTagsFilter;
    private JCheckBox checkShowAnimated;
    private JCheckBox checkShowOptimizable;
    private JButton btnResetFilters;
    private JButton btnPresetUnused;
    private JButton btnPresetComplex;
    private JButton btnPresetOptimizable;
    private JLabel labelResultCount;

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

    private void createUIComponents() {
        panelMain = new JPanel();
        panelMain.setLayout(new BorderLayout());

        // Create enhanced filter panel
        panelFilter = createEnhancedFilterPanel();
        
        // Create buttons panel
        JPanel buttonPanel = createButtonPanel();
        
        // Create north panel with better organization
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(buttonPanel, BorderLayout.NORTH);
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
        
        // Sort row
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Sort By:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        comboSort = new JComboBox<>(new String[]{
            "By Name", "By Width", "By Height", "By Width x Height", 
            "By File Size", "By Complexity", "By Usage Count", "By Tags"
        });
        panel.add(comboSort, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
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
        
        // File size filter
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Max File Size:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        sliderFileSizeMax = new JSlider(0, 50, 50); // 0-50KB
        sliderFileSizeMax.setMajorTickSpacing(10);
        sliderFileSizeMax.setMinorTickSpacing(5);
        sliderFileSizeMax.setPaintTicks(true);
        sliderFileSizeMax.setPaintLabels(true);
        sliderFileSizeMax.setToolTipText("Maximum file size in KB");
        panel.add(sliderFileSizeMax, gbc);
        
        // Tags filter
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Tags:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
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
}
