package com.hospitalqueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HospitalQueueApp extends JFrame {
    private static final Color NAVY = new Color(19, 36, 61);
    private static final Color TEAL = new Color(16, 145, 139);
    private static final Color BG = new Color(244, 248, 251);
    private static final Color TEXT = new Color(39, 55, 73);
    private static final String[] DEPARTMENTS = {"General Medicine", "Cardiology", "Orthopedics", "Pediatrics", "Dermatology"};
    private Connection conn;
    private JTable queueTable, appointmentTable;
    private DefaultTableModel queueModel, appointmentModel;
    private JLabel totalValue, waitingValue, servingValue, completedValue, servingBanner;
    private JComboBox<String> departmentFilter;
    private JTextField nameField, ageField;
    private JComboBox<String> departmentBox;
    private JTextField apptName, apptAge, apptDate, apptTime;
    private JComboBox<String> apptDepartment;
    private JTabbedPane tabs;

    public HospitalQueueApp() {
        super("CarePoint | Smart Hospital Queue");
        connectDatabase();
        buildUI();
        refreshAll();
    }

    private void connectDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:hospital_queue.db");
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS patients (id INTEGER PRIMARY KEY AUTOINCREMENT, token TEXT UNIQUE, name TEXT NOT NULL, age INTEGER, department TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'WAITING', created_at TEXT NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, appointment_id TEXT UNIQUE, name TEXT NOT NULL, age INTEGER, department TEXT NOT NULL, appt_date TEXT NOT NULL, appt_time TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'BOOKED')");
            }
        } catch (SQLException e) { showError("Could not open database: " + e.getMessage()); }
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1180, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(20, 26, 20, 26));
        JPanel brand = new JPanel();
        brand.setOpaque(false); brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CarePoint");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28)); title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("SMART HOSPITAL QUEUE MANAGEMENT");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11)); subtitle.setForeground(new Color(174, 205, 219));
        brand.add(title); brand.add(Box.createVerticalStrut(3)); brand.add(subtitle);
        JLabel today = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")));
        today.setForeground(Color.WHITE); today.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        header.add(brand, BorderLayout.WEST); header.add(today, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG); content.setBorder(new EmptyBorder(20, 24, 22, 24));
        JPanel top = new JPanel(new BorderLayout(0, 14)); top.setOpaque(false);
        JLabel welcome = new JLabel("Hospital Overview");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 22)); welcome.setForeground(NAVY);
        JLabel hint = new JLabel("Manage appointments, patient flow and consultation status in one place.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13)); hint.setForeground(new Color(103, 119, 137));
        JPanel words = new JPanel(); words.setOpaque(false); words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(welcome); words.add(Box.createVerticalStrut(5)); words.add(hint);
        top.add(words, BorderLayout.WEST);
        JPanel cards = new JPanel(new GridLayout(1,4,14,0)); cards.setOpaque(false);
        totalValue = new JLabel("0"); waitingValue = new JLabel("0"); servingValue = new JLabel("0"); completedValue = new JLabel("0");
        cards.add(statCard("TOTAL TOKENS", totalValue, new Color(57, 113, 181)));
        cards.add(statCard("WAITING", waitingValue, new Color(218, 146, 40)));
        cards.add(statCard("SERVING NOW", servingValue, TEAL));
        cards.add(statCard("COMPLETED", completedValue, new Color(76, 155, 105)));
        JPanel upper = new JPanel(new BorderLayout(0, 16)); upper.setOpaque(false);
        upper.add(top, BorderLayout.NORTH); upper.add(cards, BorderLayout.CENTER);
        servingBanner = new JLabel("  Currently serving: No patient is being served");
        servingBanner.setOpaque(true); servingBanner.setBackground(new Color(225, 245, 242)); servingBanner.setForeground(new Color(12, 105, 101));
        servingBanner.setFont(new Font("Segoe UI", Font.BOLD, 13)); servingBanner.setBorder(new EmptyBorder(12, 8, 12, 8));
        upper.add(servingBanner, BorderLayout.SOUTH);
        content.add(upper, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("  Patient Queue  ", buildQueuePanel());
        tabs.addTab("  Book Appointment  ", buildAppointmentPanel());
        tabs.addTab("  Appointments  ", buildAppointmentsPanel());
        content.add(tabs, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel statCard(String label, JLabel value, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, accent), new EmptyBorder(15, 16, 15, 16)));
        JLabel l = new JLabel(label); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(new Color(113, 128, 145));
        value.setFont(new Font("Segoe UI", Font.BOLD, 28)); value.setForeground(NAVY);
        p.add(l, BorderLayout.NORTH); p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildQueuePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(BG); p.setBorder(new EmptyBorder(14, 2, 2, 2));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); form.setBackground(Color.WHITE);
        nameField = new JTextField(12); ageField = new JTextField(4); departmentBox = new JComboBox<>(DEPARTMENTS);
        styleField(nameField); styleField(ageField);
        form.add(label("Patient name")); form.add(nameField); form.add(label("Age")); form.add(ageField);
        form.add(label("Department")); form.add(departmentBox);
        JButton register = button("＋ Register & Get Token", TEAL); register.addActionListener(e -> registerPatient());
        form.add(register);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5)); controls.setOpaque(false);
        departmentFilter = new JComboBox<>(); departmentFilter.addItem("All Departments");
        for (String d : DEPARTMENTS) departmentFilter.addItem(d);
        departmentFilter.addActionListener(e -> refreshQueue());
        controls.add(label("Filter")); controls.add(departmentFilter);
        JButton call = button("Call Next Patient", NAVY); call.addActionListener(e -> callNext());
        JButton complete = button("Complete Selected", new Color(76, 155, 105)); complete.addActionListener(e -> completeSelected());
        JButton cancel = button("Cancel Waiting Token", new Color(183, 72, 72)); cancel.addActionListener(e -> cancelSelected());
        controls.add(call); controls.add(complete); controls.add(cancel);
        JPanel north = new JPanel(new BorderLayout(0, 6)); north.setOpaque(false); north.add(form, BorderLayout.NORTH); north.add(controls, BorderLayout.SOUTH);
        p.add(north, BorderLayout.NORTH);
        queueModel = model(new String[]{"Token", "Patient", "Age", "Department", "Queue Position", "Est. Wait", "Status"});
        queueTable = new JTable(queueModel); styleTable(queueTable);
        p.add(scroll(queueTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAppointmentPanel() {
        JPanel outer = new JPanel(new BorderLayout()); outer.setBackground(BG); outer.setBorder(new EmptyBorder(18, 5, 5, 5));
        JPanel card = new JPanel(new GridBagLayout()); card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(25, 30, 25, 30));
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(9, 10, 9, 10); g.fill = GridBagConstraints.HORIZONTAL;
        JLabel heading = new JLabel("Book a Patient Appointment"); heading.setFont(new Font("Segoe UI", Font.BOLD, 20)); heading.setForeground(NAVY);
        g.gridx=0; g.gridy=0; g.gridwidth=2; card.add(heading,g); g.gridwidth=1;
        apptName = new JTextField(20); apptAge = new JTextField(20); apptDepartment = new JComboBox<>(DEPARTMENTS);
        apptDate = new JTextField(LocalDate.now().plusDays(1).toString(),20);
        apptTime = new JTextField("10:00",20);
        addFormRow(card,g,1,"Patient name",apptName); addFormRow(card,g,2,"Age",apptAge); addFormRow(card,g,3,"Department",apptDepartment);
        addFormRow(card,g,4,"Date (YYYY-MM-DD)",apptDate); addFormRow(card,g,5,"Time (24-hour HH:MM)",apptTime);
        JButton book = button("Confirm Appointment", TEAL); book.addActionListener(e -> bookAppointment());
        g.gridx=1; g.gridy=6; card.add(book,g);
        JLabel note = new JLabel("Please use a future date and a valid time. Appointment slots are subject to hospital confirmation.");
        note.setForeground(new Color(103,119,137)); note.setFont(new Font("Segoe UI",Font.PLAIN,12));
        g.gridx=0; g.gridy=7; g.gridwidth=2; card.add(note,g);
        outer.add(card, BorderLayout.NORTH);
        return outer;
    }

    private JPanel buildAppointmentsPanel() {
        JPanel p = new JPanel(new BorderLayout(0,12)); p.setBackground(BG); p.setBorder(new EmptyBorder(16, 2, 2, 2));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT)); bar.setOpaque(false);
        JButton refresh = button("Refresh Appointments", NAVY); refresh.addActionListener(e -> refreshAppointments());
        JButton cancel = button("Cancel Selected Booking", new Color(183,72,72)); cancel.addActionListener(e -> cancelAppointment());
        bar.add(refresh); bar.add(cancel); p.add(bar, BorderLayout.NORTH);
        appointmentModel = model(new String[]{"Appointment ID","Patient","Age","Department","Date","Time","Status"});
        appointmentTable = new JTable(appointmentModel); styleTable(appointmentTable);
        p.add(scroll(appointmentTable), BorderLayout.CENTER);
        return p;
    }

    private void addFormRow(JPanel panel, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridy=row; g.gridx=0; g.weightx=0; panel.add(label(label),g);
        g.gridx=1; g.weightx=1; panel.add(field,g);
        if (field instanceof JTextField tf) styleField(tf);
    }
    private JLabel label(String s) { JLabel l = new JLabel(s); l.setForeground(TEXT); l.setFont(new Font("Segoe UI",Font.PLAIN,12)); return l; }
    private void styleField(JTextField f) { f.setFont(new Font("Segoe UI",Font.PLAIN,13)); f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(211,221,230)),new EmptyBorder(7,8,7,8))); }
    private JButton button(String text, Color color) {
        JButton b = new JButton(text); b.setFont(new Font("Segoe UI",Font.BOLD,12)); b.setForeground(Color.WHITE); b.setBackground(color);
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(9,13,9,13)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private DefaultTableModel model(String[] cols) { return new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} }; }
    private JScrollPane scroll(JTable t) { JScrollPane s = new JScrollPane(t); s.setBorder(BorderFactory.createLineBorder(new Color(222,230,237))); return s; }
    private void styleTable(JTable t) {
        t.setRowHeight(31); t.setFont(new Font("Segoe UI",Font.PLAIN,12)); t.setForeground(TEXT); t.setGridColor(new Color(231,237,242));
        t.setSelectionBackground(new Color(215,239,236)); t.setSelectionForeground(NAVY); t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); t.getTableHeader().setBackground(new Color(232,240,246)); t.getTableHeader().setForeground(NAVY);
    }

    private void registerPatient() {
        String name = nameField.getText().trim(), ageText = ageField.getText().trim(), dept = (String)departmentBox.getSelectedItem();
        if(name.isEmpty() || ageText.isEmpty()) { showError("Enter patient name and age."); return; }
        try {
            int age = Integer.parseInt(ageText); if(age < 0 || age > 120) throw new NumberFormatException();
            String token = tokenPrefix(dept) + String.format("%03d", nextId("patients"));
            try(PreparedStatement ps=conn.prepareStatement("INSERT INTO patients(token,name,age,department,status,created_at) VALUES(?,?,?,?,?,?)")) {
                ps.setString(1,token); ps.setString(2,name); ps.setInt(3,age); ps.setString(4,dept); ps.setString(5,"WAITING"); ps.setString(6,LocalDateTime.now().toString()); ps.executeUpdate();
            }
            nameField.setText(""); ageField.setText(""); refreshAll();
            JOptionPane.showMessageDialog(this,"Patient registered successfully. Token: "+token,"Token Generated",JOptionPane.INFORMATION_MESSAGE);
        } catch(NumberFormatException ex){showError("Age must be a number between 0 and 120.");} catch(SQLException ex){showError(ex.getMessage());}
    }
    private int nextId(String table) throws SQLException { try(Statement st=conn.createStatement(); ResultSet rs=st.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM "+table)){return rs.next()?rs.getInt(1):1;} }
    private String tokenPrefix(String d) { if(d.startsWith("Cardiology"))return "CA"; if(d.startsWith("Orthopedics"))return "OR"; if(d.startsWith("Pediatrics"))return "PE"; if(d.startsWith("Dermatology"))return "DE"; return "GM"; }

    private void callNext() {
        String dept = selectedDepartment();
        if(dept == null) { showError("Choose a specific department in the Filter dropdown before calling the next patient."); return; }
        try {
            if(countStatus(dept,"SERVING")>0) { showError("A patient is already SERVING in "+dept+". Complete that consultation first."); return; }
            try(PreparedStatement ps=conn.prepareStatement("SELECT id FROM patients WHERE department=? AND status='WAITING' ORDER BY id LIMIT 1")) {
                ps.setString(1,dept); try(ResultSet rs=ps.executeQuery()) {
                    if(!rs.next()){JOptionPane.showMessageDialog(this,"No waiting patients in "+dept+".");return;}
                    try(PreparedStatement up=conn.prepareStatement("UPDATE patients SET status='SERVING' WHERE id=?")){up.setInt(1,rs.getInt(1));up.executeUpdate();}
                }
            }
            refreshAll();
        } catch(SQLException ex){showError(ex.getMessage());}
    }
    private void completeSelected() {
        int row=queueTable.getSelectedRow(); if(row<0){showError("Select the SERVING patient in the queue table first.");return;}
        String token=(String)queueModel.getValueAt(queueTable.convertRowIndexToModel(row),0);
        try(PreparedStatement ps=conn.prepareStatement("UPDATE patients SET status='COMPLETED' WHERE token=? AND status='SERVING'")) {
            ps.setString(1,token); int n=ps.executeUpdate();
            if(n==0) showError("Only a patient with SERVING status can be completed."); else refreshAll();
        } catch(SQLException ex){showError(ex.getMessage());}
    }
    private void cancelSelected() {
        int row=queueTable.getSelectedRow(); if(row<0){showError("Select a patient row first.");return;}
        String token=(String)queueModel.getValueAt(queueTable.convertRowIndexToModel(row),0);
        try(PreparedStatement ps=conn.prepareStatement("UPDATE patients SET status='CANCELLED' WHERE token=? AND status='WAITING'")) {
            ps.setString(1,token); int n=ps.executeUpdate();
            if(n==0)showError("Only WAITING tokens can be cancelled.");else refreshAll();
        }catch(SQLException ex){showError(ex.getMessage());}
    }
    private void bookAppointment() {
        String name=apptName.getText().trim(), ageText=apptAge.getText().trim(), dept=(String)apptDepartment.getSelectedItem();
        try {
            if(name.isEmpty()||ageText.isEmpty()) {showError("Enter patient name and age.");return;}
            int age=Integer.parseInt(ageText); if(age<0||age>120)throw new NumberFormatException();
            LocalDate date=LocalDate.parse(apptDate.getText().trim());
            LocalTime time=LocalTime.parse(apptTime.getText().trim());
            if(LocalDateTime.of(date,time).isBefore(LocalDateTime.now().plusMinutes(1))) {showError("Choose a future date and time.");return;}
            String id="AP"+String.format("%04d",nextId("appointments"));
            try(PreparedStatement ps=conn.prepareStatement("INSERT INTO appointments(appointment_id,name,age,department,appt_date,appt_time,status) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1,id);ps.setString(2,name);ps.setInt(3,age);ps.setString(4,dept);ps.setString(5,date.toString());ps.setString(6,time.toString());ps.setString(7,"BOOKED");ps.executeUpdate();
            }
            apptName.setText("");apptAge.setText("");refreshAll();tabs.setSelectedIndex(2);
            JOptionPane.showMessageDialog(this,"Appointment booked successfully!\nAppointment ID: "+id,"Booking Confirmed",JOptionPane.INFORMATION_MESSAGE);
        } catch(NumberFormatException ex){showError("Age must be a number between 0 and 120.");} catch(Exception ex){showError("Check date as YYYY-MM-DD and time as HH:MM (24-hour).");}
    }
    private void cancelAppointment() {
        int row=appointmentTable.getSelectedRow(); if(row<0){showError("Select an appointment first.");return;}
        String id=(String)appointmentModel.getValueAt(appointmentTable.convertRowIndexToModel(row),0);
        try(PreparedStatement ps=conn.prepareStatement("UPDATE appointments SET status='CANCELLED' WHERE appointment_id=? AND status='BOOKED'")) {
            int n=ps.executeUpdate(); if(n==0)showError("Only BOOKED appointments can be cancelled.");else refreshAppointments();
        }catch(SQLException ex){showError(ex.getMessage());}
    }
    private String selectedDepartment() { Object o=departmentFilter.getSelectedItem(); return o==null||o.equals("All Departments")?null:o.toString(); }
    private int countStatus(String dept,String status)throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT COUNT(*) FROM patients WHERE department=? AND status=?")){ps.setString(1,dept);ps.setString(2,status);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):0;}}
    }
    private void refreshAll(){refreshQueue();refreshAppointments();refreshStats();}
    private void refreshQueue() {
        if(queueModel==null||conn==null)return; queueModel.setRowCount(0);
        String dept=selectedDepartment();
        String sql="SELECT token,name,age,department,status FROM patients WHERE status IN ('WAITING','SERVING','COMPLETED','CANCELLED')"+(dept==null?"":" AND department=?")+" ORDER BY CASE status WHEN 'SERVING' THEN 0 WHEN 'WAITING' THEN 1 ELSE 2 END,id";
        try(PreparedStatement ps=conn.prepareStatement(sql)) {
            if(dept!=null)ps.setString(1,dept);
            try(ResultSet rs=ps.executeQuery()) {
                List<String[]> rows=new ArrayList<>();
                while(rs.next()) {
                    String token=rs.getString("token"), name=rs.getString("name"), department=rs.getString("department"), status=rs.getString("status");
                    int age=rs.getInt("age");
                    int pos=0; if(status.equals("WAITING")) { try(PreparedStatement qp=conn.prepareStatement("SELECT COUNT(*) FROM patients WHERE department=? AND status='WAITING' AND id <= (SELECT id FROM patients WHERE token=?)")){qp.setString(1,department);qp.setString(2,token);try(ResultSet qr=qp.executeQuery()){if(qr.next())pos=qr.getInt(1);}}}
                    String wait=status.equals("SERVING")?"Now":status.equals("WAITING")?((pos-1)*10)+" min":"—";
                    queueModel.addRow(new Object[]{token,name,age,department,status.equals("WAITING")?pos:"—",wait,status});
                }
            }
        }catch(SQLException ex){showError(ex.getMessage());}
    }
    private void refreshAppointments() {
        if(appointmentModel==null||conn==null)return; appointmentModel.setRowCount(0);
        try(Statement st=conn.createStatement();ResultSet rs=st.executeQuery("SELECT appointment_id,name,age,department,appt_date,appt_time,status FROM appointments ORDER BY appt_date,appt_time")) {
            while(rs.next())appointmentModel.addRow(new Object[]{rs.getString(1),rs.getString(2),rs.getInt(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)});
        }catch(SQLException ex){showError(ex.getMessage());}
    }
    private void refreshStats() {
        if(conn==null)return;
        try(Statement st=conn.createStatement();ResultSet rs=st.executeQuery("SELECT COUNT(*),SUM(status='WAITING'),SUM(status='SERVING'),SUM(status='COMPLETED') FROM patients")) {
            if(rs.next()){totalValue.setText(rs.getString(1));waitingValue.setText(String.valueOf(rs.getInt(2)));servingValue.setText(String.valueOf(rs.getInt(3)));completedValue.setText(String.valueOf(rs.getInt(4)));}
        }catch(SQLException ex){showError(ex.getMessage());}
        try(Statement st=conn.createStatement();ResultSet rs=st.executeQuery("SELECT name,token,department FROM patients WHERE status='SERVING' ORDER BY id LIMIT 1")) {
            if(rs.next())servingBanner.setText("  Currently serving: "+rs.getString(1)+"  •  "+rs.getString(2)+"  •  "+rs.getString(3));
            else servingBanner.setText("  Currently serving: No patient is being served");
        }catch(SQLException ex){showError(ex.getMessage());}
    }
    private void showError(String msg){JOptionPane.showMessageDialog(this,msg,"CarePoint",JOptionPane.WARNING_MESSAGE);}
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ignored) {}
        SwingUtilities.invokeLater(() -> new HospitalQueueApp().setVisible(true));
    }
}
