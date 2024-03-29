package com.igormaznitsa.ravikoodi;

import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.lang.NonNull;

public class AboutPanel extends javax.swing.JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(AboutPanel.class);
  
  public AboutPanel(@NonNull final DonationController donationController, @NonNull final BuildProperties buildProperties) {
    initComponents();
    this.labelText.setText(this.labelText.getText()
            .replace("${version}", buildProperties.getVersion())
            .replace("${title}", "Ravikoodi content server")
    );
    this.labelText.addLinkListener((@NonNull final JHtmlLabel source, @NonNull final String link) -> {
      if ("open_donation".equalsIgnoreCase(link)) {
        donationController.openDonationUrl();
      } else {
        try{
          Utils.showURLExternal(new URL(link));
        }catch(MalformedURLException ex){
          LOGGER.error("detected malformed URL: {}", link, ex);
        }
      }
    });
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        labelIcon = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        labelText = new com.igormaznitsa.ravikoodi.JHtmlLabel();

        setLayout(new java.awt.GridBagLayout());

        labelIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/64_app_icon.png"))); // NOI18N
        labelIcon.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        add(labelIcon, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(32, 8, 8, 8));
        jPanel1.setLayout(new java.awt.BorderLayout());

        labelText.setText("<html>\n<h2>${title}</h2>\n<hr>\n<p>Version: ${version}</p><br>\n<ul>\n<li>Author: <a href=\"https://www.igormaznitsa.com/\">Igor Maznitsa</a></li>\n<li>Projet page: <a href=\"https://github.com/raydac/ravikoodi-server\">https://github.com/raydac/ravikoodi-server</a></li>\n<li>License: <a href=\"https://github.com/raydac/kodi-videoserver-app/blob/master/LICENSE\">Apache 2.0</a></li>\n<hr>\n<p>\nSmall server to broadcast local media-content to <a href=\"https://kodi.tv/\">KODI media player</a>. It supports HTTP and HTTPS conections.<br><br>\n3th side required software:\n<ul>\n<li><a href=\"https://bell-sw.com/\">Java 11+</a> for work</li>\n<li><a href=\"http://ffmpeg.org/download.html\">FFmpeg</a> for screencast functionality</li>\n</ul>\n</p>\n<br>\n<p>Application icons provided by <b><a href=\"https://www.fatcow.com/free-icons\">FatCow</a></b></p>\n<br>\n<p>\n<b>If the application is useful for you, you can <a href=\"open_donation\">make some donation</a>,</b>\n</p>\n</html>");
        labelText.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jPanel1.add(labelText, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1000.0;
        gridBagConstraints.weighty = 1000.0;
        add(jPanel1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel labelIcon;
    private com.igormaznitsa.ravikoodi.JHtmlLabel labelText;
    // End of variables declaration//GEN-END:variables
}
