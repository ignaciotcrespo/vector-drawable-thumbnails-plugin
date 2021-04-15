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
        panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.X_AXIS));

        panelVectors = new JPanel();
        GridLayout gridLayout = new GridLayout(0, 3);
        gridLayout.setVgap(20);
        panelVectors.setLayout(gridLayout);

        vectorsContainer = new JBScrollPane(panelVectors);
        vectorsContainer.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        vectorsContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        vectorsContainer.setLayout(new ScrollPaneLayout());

//        btnDonate.setOpaque(false);
//        btnDonate.setContentAreaFilled(false);
//        btnDonate.setBorderPainted(false);

        // https://jetbrains.design/intellij/resources/icons_list/
//        btnDonate = new JButton(AllIcons.Nodes.Gvariable);
    }
}
