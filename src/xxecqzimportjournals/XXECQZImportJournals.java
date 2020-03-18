/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xxecqzimportjournals;

import com.csvreader.CsvReader;
import fusion.common.ErpIntegrationService;
import fusion.common.ReportService;
import fusion.login.Connection;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import radical.utl.UtlCSV;
import radical.utl.UtlFile;
import radical.utl.UtlSOAP;
import fusion.generalledger.Journal;
import fusion.generalledger.JournalLine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import radical.common.Result;
import radical.utl.UtlSystem;

/**
 *
 * @author Menez
 */
public class XXECQZImportJournals {

    static Journal journal = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        // TODO code application logic here
        Connection.setServiceInstance("ecqz-test");
        Connection.setDataCenterCode("us2");
        Connection.fusionVersion = 13;
        Connection.userName = "FIN_IMPL";
        Connection.password = "Mexico01";
        UtlSOAP.logEnabled = true;

        //
        int[] outResultCode = {2};
        String[] outResultText = {""};

        //
        Journal ljournal = getJournal();
        if (ljournal != null) {
            ljournal.sendToInterface(outResultCode, outResultText);
            System.out.println(Result.decode(outResultCode[0]) + ": " + outResultText[0]);
            String dataAccessSetId = getIdDataAccessSet("HCC_UN");
            importJournal(dataAccessSetId, "300000035752222", "300000003402964",ljournal.groupId, outResultCode, outResultText);
        }
    }

    public static Journal getJournal() throws FileNotFoundException {

        String inputFileName = "C:\\Users\\Menez\\Downloads\\FVSH0306.POL";
        //String outPutFileName = UtlFile.replaceFileExtension(inputFileName, UtlFile.getDateTimeAsName() + ".csv");
        //FileOutputStream outputFileStream = new FileOutputStream(outPutFileName); 
        // UtlCSV.writeRecord(outputFileStream, "Bussines Unit Name", "Invoice Number","Supplier Number","Result","Message","Envelope");
        String userCategoryName, userSourceName, chartOfAccountsId, ledgerId, journalName, tipoPoliza, cuentaContable, accountingDateStr, numeroDocumento, descripcionPoliza, anio, mes, dia;
        char tipoMovimiento;
        double importeMovimiento;
        Date DaccountingDateStr;
        String line;
        CsvReader reader = new CsvReader(inputFileName);
        try {
            //reader.readHeaders();
            while (reader.readRecord()) {
                line = reader.getRawRecord();
                tipoPoliza = line.substring(0, 2);
                cuentaContable = line.substring(2, 15);
                anio = line.substring(15, 19);
                mes = line.substring(19, 21);
                dia = line.substring(21, 23);
                numeroDocumento = line.substring(23, 28);
                descripcionPoliza = line.substring(28, 63);
                tipoMovimiento = line.substring(63, 64).equals("0") ? 'D' : 'C';
                importeMovimiento = Double.parseDouble(line.substring(64, 77) + "." + line.substring(77, 79));
                accountingDateStr = anio + "-" + mes + "-" + dia;
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                DaccountingDateStr = formatter.parse(accountingDateStr);
                accountingDateStr = formatter.format(DaccountingDateStr);
                Date today = Calendar.getInstance().getTime();
                journalName = "Journal " + today.toString();
                ledgerId = "300000003402964";
                userSourceName = "COSTOS";
                userCategoryName = "COSTOS";
                chartOfAccountsId = "81";

                JournalLine journalLine = new JournalLine(tipoMovimiento, descripcionPoliza, String.valueOf(importeMovimiento), "070", "000", "000", "219600020", "000", "000", "", "", "", "");

                if (journal == null) {
                    journal = new Journal(journalName, accountingDateStr, ledgerId, chartOfAccountsId, userSourceName, userCategoryName);
                }
                journal.addLine(journalLine);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            reader.close();
            //outputFileStream.close();
        }

        return journal;
    }

    public static void importJournal(String dataAccessSetId, String source, String Ledger, String groupId, int[] outResultCode, String[] outResultText) {
        try {
            String jobPackageName = "/oracle/apps/ess/financials/generalLedger/programs/common/";
            String jobDefinitionName = "JournalImportLauncher";
            String[] paramList = {
                dataAccessSetId,
                source,
                Ledger,
                groupId,
                "N",
                "N",
                "N",};
            //
            System.out.println("Enviando solicitud de proceso programado 'Importar Polizas' (Import Journals)");
            //
            ErpIntegrationService.submitESSJobRequest(jobPackageName, jobDefinitionName, paramList,
                    outResultCode, outResultText);
            //
            System.out.println("RESULT CODE: " + outResultCode[0]);
            System.out.println("RESULT TEXT: " + outResultText[0]);
            //
            if (outResultCode[0] == 0) {
                String requestId = outResultText[0];
                do {
                    System.out.println("Esperando finalice el procesamiento de la solicitud...");
                    UtlSystem.sleep(10);//UtlSystem.sleep(5);
                    //
                    ErpIntegrationService.getESSJobStatus(requestId, outResultCode, outResultText);
                    //
                    System.out.println("RESULT CODE: " + outResultCode[0]);
                    System.out.println("RESULT TEXT: " + outResultText[0]);
                } while (!outResultText[0].equals(ErpIntegrationService.SUCCEEDED)
                        && !outResultText[0].equals(ErpIntegrationService.WARNING)
                        && !outResultText[0].equals(ErpIntegrationService.ERROR)
                        && !outResultText[0].equals(ErpIntegrationService.CANCELED));
                //
                if (outResultText[0].equals(ErpIntegrationService.SUCCEEDED)) {
                    outResultCode[0] = 0; // Success
                } else if (outResultText[0].equals(ErpIntegrationService.WARNING)) {
                    outResultCode[0] = 1; // Warning
                } else {
                    outResultCode[0] = 2; // Error
                }
            }
        } catch (Exception e) {
            outResultCode[0] = 3; // Exception
            outResultText[0] = e.toString();
        }

    }

    public static String getIdDataAccessSet(String dataAccessSet) throws IOException {
        String IdDataAccess = "";
        String reportAbsolutePath = "/Custom/XX/XX_GL_DATA_ACCESS_REP.xdo";
            String[] params = {"P_NAME", dataAccessSet};
        byte[] reportBytes = ReportService.runReport(reportAbsolutePath, "csv", params);
        CsvReader reader = new CsvReader(new ByteArrayInputStream(reportBytes), Charset.forName("UTF-8"));
        reader.readHeaders();
        while (reader.readRecord()) {
            IdDataAccess = reader.get(2);
        }
        return IdDataAccess;
    }
}
