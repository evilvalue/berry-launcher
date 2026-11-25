package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Auth;
import ru.berrytrip.berryup.Log;
import ru.berrytrip.berryup.Skins;
import ru.berrytrip.berryup.Store;
import ru.berrytrip.berryup.Sys;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

/** Вкладка «Аккаунт»: вход через Telegram-бота и скин QuickSkin. */
public class AccountPage extends JPanel {

    private final MainFrame host;
    private final JLabel status;
    private final JTextArea hint;
    private final JLabel skinStatus = Widgets.label("", Palette.reg(8.5f), Palette.MUTED);
    private final JTextField nickField;
    private final Widgets.BerryButton login;
    private final Widgets.BerryButton logout;
    private final Widgets.BerryButton openBot;
    private final Widgets.BerryButton check;
    private final Widgets.BerryButton applySkin;
    private final JCheckBox remember;
    private final SkinView preview;

    private File pickedSkin;
    private int lastState = -1;

    public AccountPage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // ---------------------------------------------------------- вход
        Widgets.Card loginCard = new Widgets.Card();
        loginCard.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.pad(18, 20, 18, 20);
        loginCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        loginCard.add(left(Widgets.caption("ВХОД ЧЕРЕЗ TELEGRAM", Palette.SURFACE)));
        loginCard.add(Box.createVerticalStrut(8));

        status = Widgets.label("Вход не выполнен", Palette.semi(12f), Palette.WARN);
        loginCard.add(left(status));
        loginCard.add(Box.createVerticalStrut(4));

        hint = Widgets.paragraph(
                "Нажми «Войти» — откроется бот. Нажми в нём Start, и сервер пустит в игру сам.",
                Palette.reg(9f), Palette.MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginCard.add(hint);
        loginCard.add(Box.createVerticalStrut(12));

        JPanel nickRow = new JPanel(new BorderLayout(8, 0));
        nickRow.setOpaque(false);
        nickRow.setAlignmentX(LEFT_ALIGNMENT);
        nickRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        nickField = new JTextField();
        nickField.setFont(Palette.reg(9.5f));
        nickField.setBackground(Palette.SURFACE_ALT);
        nickField.setForeground(Palette.FG);
        nickField.setCaretColor(Palette.FG);
        nickField.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        nickField.setToolTipText("Ник в Minecraft, который нужно привязать к Telegram");
        nickRow.add(nickField, BorderLayout.CENTER);
        login = new Widgets.BerryButton("Войти через Telegram", Widgets.BerryButton.Kind.PRIMARY);
        login.addActionListener(e -> startLogin());
        nickRow.add(login, BorderLayout.EAST);
        loginCard.add(nickRow);
        loginCard.add(Box.createVerticalStrut(8));

        remember = new JCheckBox("Запомнить меня на этом компьютере");
        remember.setOpaque(false);
        remember.setFont(Palette.reg(9f));
        remember.setForeground(Palette.MUTED);
        remember.setFocusPainted(false);
        remember.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        remember.setAlignmentX(LEFT_ALIGNMENT);
        remember.setSelected(Store.remember());
        remember.setToolTipText("Токен входа лежит в сборке. Выключи на общем компьютере — тогда при следующем запуске BerryUp попросит вход заново.");
        remember.addActionListener(e -> {
            Store.setRemember(remember.isSelected());
            Store.save();
            host.setState(remember.isSelected()
                            ? "Вход сохранится на этом компьютере"
                            : "Вход не сохранится: при следующем запуске попросим заново",
                    Palette.MUTED);
            refresh();
        });
        loginCard.add(remember);
        loginCard.add(Box.createVerticalStrut(8));

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(LEFT_ALIGNMENT);
        bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel fine = Widgets.label(
                "Вход без кодов: ссылка из лаунчера + Start в боте. Ссылку никому не пересылай.",
                Palette.reg(8f), Palette.DIM);
        bottomRow.add(fine, BorderLayout.WEST);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        openBot = new Widgets.BerryButton("Открыть бота", Widgets.BerryButton.Kind.SECONDARY);
        openBot.addActionListener(e -> startLogin());
        check = new Widgets.BerryButton("Проверить вход", Widgets.BerryButton.Kind.GHOST);
        check.addActionListener(e -> {
            refresh();
            int state = Auth.tokenState(host.prism());
            host.setState(tokenMessage(state), state == Auth.TOKEN_OK ? Palette.OK : Palette.WARN);
        });
        logout = new Widgets.BerryButton("Выйти", Widgets.BerryButton.Kind.GHOST);
        logout.addActionListener(e -> {
            Auth.logout(host.prism());
            refresh();
            host.setState("Вход сброшен", Palette.MUTED);
        });
        actions.add(openBot);
        actions.add(check);
        actions.add(logout);
        bottomRow.add(actions, BorderLayout.EAST);
        loginCard.add(bottomRow);
        add(loginCard);
        add(Box.createVerticalStrut(12));

        // ---------------------------------------------------------- скин
        Widgets.Card skinCard = new Widgets.Card();
        skinCard.setAlignmentX(LEFT_ALIGNMENT);
        skinCard.setLayout(new BorderLayout(16, 0));
        skinCard.pad(18, 20, 18, 20);
        skinCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 336));
        skinCard.add(left(Widgets.caption("СКИН · 3D-ПРОСМОТР", Palette.SURFACE)), BorderLayout.NORTH);

        preview = new SkinView();
        preview.setPreferredSize(new Dimension(190, 252));
        preview.setMinimumSize(new Dimension(130, 190));
        BufferedImage existing = Skins.current(host.prism());
        preview.setSkin(existing);
        skinStatus.setText(existing != null
                ? "текущий скин · потяни модель мышью"
                : "скин не выбран · выбери PNG или перетащи файл");
        preview.setTransferHandler(new javax.swing.TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<File> files = (java.util.List<File>) support.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (files.isEmpty()) return false;
                    useSkin(files.get(0));
                    return true;
                } catch (Exception error) {
                    skinStatus.setText("Не удалось принять файл: " + error.getMessage());
                    return false;
                }
            }
        });
        skinCard.add(preview, BorderLayout.WEST);

        JPanel skinText = new JPanel();
        skinText.setOpaque(false);
        skinText.setLayout(new BoxLayout(skinText, BoxLayout.Y_AXIS));
        JTextArea skinHint = Widgets.paragraph(
                "Выбери PNG 64×64 или HD 128×128 — модель сразу покажет, как скин сядет. "
                        + "Можно просто перетащить файл на модель. Скин попадёт в папку QuickSkin и применится модом.",
                Palette.reg(9f), Palette.MUTED);
        skinHint.setAlignmentX(LEFT_ALIGNMENT);
        skinHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        skinText.add(skinHint);
        skinText.add(Box.createVerticalStrut(10));

        JPanel skinButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        skinButtons.setOpaque(false);
        skinButtons.setAlignmentX(LEFT_ALIGNMENT);
        Widgets.BerryButton pick = new Widgets.BerryButton("Выбрать PNG…", Widgets.BerryButton.Kind.SECONDARY);
        pick.addActionListener(e -> pickSkin());
        applySkin = new Widgets.BerryButton("Применить", Widgets.BerryButton.Kind.PRIMARY);
        applySkin.setEnabled(false);
        applySkin.addActionListener(e -> applySkin());
        skinButtons.add(pick);
        skinButtons.add(applySkin);
        skinText.add(skinButtons);

        skinText.add(Box.createVerticalStrut(8));
        skinText.add(left(skinStatus));
        skinCard.add(skinText, BorderLayout.CENTER);
        add(skinCard);
        add(Box.createVerticalGlue());

        refresh();

        // токен пишет игра после входа — лаунчер сам подхватит новое состояние
        javax.swing.Timer poll = new javax.swing.Timer(2500, event -> {
            int state = Auth.tokenState(host.prism());
            if (state == lastState) {
                return;
            }
            refresh();
            if (state == Auth.TOKEN_OK) {
                host.setState("Вход подтверждён — пустим в игру", Palette.OK);
            }
        });
        poll.start();
    }

    private JLabel left(JLabel label) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    public void refresh() {
        int state = Auth.tokenState(host.prism());
        lastState = state;
        String nick = Auth.tokenNick(host.prism());
        if (nick.isEmpty()) nick = Auth.nick();
        if (state == Auth.TOKEN_OK) {
            status.setText("Вход выполнен" + (nick.isEmpty() ? "" : " · " + nick));
            status.setForeground(Palette.OK);
            hint.setText("Вход подтверждён: токен в сборке, сервер пустит сразу.");
        } else {
            status.setText(state == Auth.TOKEN_EXPIRED ? "Сессия истекла — войди снова"
                    : state == Auth.TOKEN_OTHER_NICK ? "Токен выдан другому нику"
                    : state == Auth.TOKEN_BROKEN ? "Токен повреждён — войди снова"
                    : "Вход не выполнен");
            status.setForeground(Palette.WARN);
            hint.setText(state == Auth.TOKEN_NONE
                    ? "Нажми «Войти» — откроется бот, нажми Start и запускай игру."
                    : "Нажми «Войти» ещё раз: сохранённый токен больше не подходит.");
        }
        boolean signed = state == Auth.TOKEN_OK;
        login.setText(signed ? "Войти заново" : "Войти через Telegram");
        logout.setVisible(signed);
        remember.setSelected(Store.remember());
        if (nickField.getText().trim().isEmpty()) {
            String name = Auth.nick();
            if (name.isEmpty() && host.prism() != null) name = host.prism().accountName();
            nickField.setText(name);
        }
        revalidate();
        repaint();
    }

    private String tokenMessage(int state) {
        if (state == Auth.TOKEN_OK) return "Вход подтверждён";
        if (state == Auth.TOKEN_EXPIRED) return "Токен истёк — войди снова";
        if (state == Auth.TOKEN_OTHER_NICK) return "Токен выдан другому нику";
        if (state == Auth.TOKEN_BROKEN) return "Файл токена повреждён";
        return "Токена пока нет";
    }

    private void startLogin() {
        String nick = nickField.getText().trim();
        if (nick.isEmpty()) {
            status.setText("Напиши ник из Minecraft");
            status.setForeground(Palette.WARN);
            return;
        }
        String error = Auth.start(host.prism(), nick);
        if (!error.isEmpty()) {
            status.setText(error);
            status.setForeground(Palette.WARN);
            return;
        }
        status.setText("Открыл бота в браузере");
        status.setForeground(Palette.ACCENT_HOVER);
        hint.setText("Ссылка выдана для «" + nick + "». Нажми Start в боте — и запускай игру, "
                + "сервер пустит автоматически.");
    }

    private void pickSkin() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("PNG со скином 64×64 (или HD 128×128)");
        chooser.setFileFilter(new FileNameExtensionFilter("Скин (*.png)", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        useSkin(chooser.getSelectedFile());
    }

    /** Один путь для кнопки и для drag&drop: проверить PNG и показать в 3D. */
    private void useSkin(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                skinStatus.setText("Это не PNG-картинка");
                return;
            }
            pickedSkin = file;
            preview.setSkin(image);
            applySkin.setEnabled(true);
            skinStatus.setText(file.getName() + " · " + image.getWidth() + "×" + image.getHeight()
                    + " · покрути модель мышью");
        } catch (Exception error) {
            skinStatus.setText("Не удалось прочитать файл: " + error.getMessage());
        }
    }

    private void applySkin() {
        if (pickedSkin == null) return;
        applySkin.setEnabled(false);
        skinStatus.setText("Копирую…");
        new Thread(() -> {
            String message;
            try {
                String name = Skins.apply(host.prism(), pickedSkin, Auth.nick());
                message = "Скин загружен: " + name;
                Log.ok("Скин добавлен в QuickSkin: " + name);
            } catch (Exception error) {
                message = "Не получилось: " + error.getMessage();
                Log.error(message);
            }
            final String text = message;
            SwingUtilities.invokeLater(() -> {
                applySkin.setEnabled(true);
                skinStatus.setText(text + " · если не применился, выбери его в меню QuickSkin");
                host.setState(text, text.startsWith("Скин") ? Palette.OK : Palette.ERR);
            });
        }, "berry-skin").start();
    }
}
