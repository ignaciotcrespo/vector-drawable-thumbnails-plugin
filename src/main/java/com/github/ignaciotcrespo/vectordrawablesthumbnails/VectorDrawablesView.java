package com.github.ignaciotcrespo.vectordrawablesthumbnails;

import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
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

    public VectorDrawablesView() {
        initializeComponents();
    }

    private void initializeComponents() {
        if (panelMain == null) {
            createUIComponents();
        }
        
        // Initialize combo boxes with default values
        if (comboSort != null) {
            comboSort.setSelectedItem("By Name");
        }
        if (comboSortDirection != null) {
            comboSortDirection.setSelectedItem("Asc");
        }
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

    private void createUIComponents() {
        panelMain = new JPanel();
        panelMain.setLayout(new BorderLayout());

        // Create filter panel
        panelFilter = new JPanel();
        panelFilter.setLayout(new BorderLayout());
        
        // Create filter components
        JPanel filterRow = new JPanel(new BorderLayout());
        filterRow.add(new JLabel("Filter"), BorderLayout.WEST);
        textFilter = new JTextField();
        filterRow.add(textFilter, BorderLayout.CENTER);
        clearButton = new JButton("Clear");
        filterRow.add(clearButton, BorderLayout.EAST);
        
        // Create sort components
        JPanel sortRow = new JPanel(new BorderLayout());
        sortRow.add(new JLabel("Sort By"), BorderLayout.WEST);
        comboSort = new JComboBox<>(new String[]{"Unsorted", "By Name", "By Width", "By Height", "By Width x Height", "By File Size"});
        sortRow.add(comboSort, BorderLayout.CENTER);
        comboSortDirection = new JComboBox<>(new String[]{"Asc", "Desc"});
        sortRow.add(comboSortDirection, BorderLayout.EAST);
        
        panelFilter.add(sortRow, BorderLayout.NORTH);
        panelFilter.add(filterRow, BorderLayout.CENTER);
        
        // Create buttons
        JPanel buttonPanel = new JPanel(new BorderLayout());
        btnRefresh = new JButton("Refresh");
        buttonPanel.add(btnRefresh, BorderLayout.CENTER);
        btnDonate = new JButton("♡");
        buttonPanel.add(btnDonate, BorderLayout.EAST);
        
        // Create north panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(panelFilter, BorderLayout.SOUTH);
        northPanel.add(buttonPanel, BorderLayout.CENTER);
        
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
}
