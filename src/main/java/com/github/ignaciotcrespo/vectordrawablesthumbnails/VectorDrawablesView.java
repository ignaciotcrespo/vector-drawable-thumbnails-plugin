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
    private JRadioButton radioSortName;
    private JRadioButton radioSortUnsorted;

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

    public JRadioButton getRadioSortName() {
        return radioSortName;
    }

    public JRadioButton getRadioSortUnsorted() {
        return radioSortUnsorted;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    private void createUIComponents() {
        panelMain = new JPanel();
        panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.PAGE_AXIS));

        panelVectors = new JPanel();
        panelVectors.setLayout(new GridLayout(0, 3));

        vectorsContainer = new JBScrollPane(panelVectors);
        vectorsContainer.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        vectorsContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        vectorsContainer.setLayout(new ScrollPaneLayout());

    }
}
