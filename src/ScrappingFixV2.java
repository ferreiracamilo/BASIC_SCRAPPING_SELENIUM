import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;





public class ScrappingFixV2 extends JPanel{
		
	public static void main(String[] args) throws InterruptedException, IOException {
		// TODO Auto-generated method stub
		List<String> fechas= guiFechas();//pos cero fecha ini y pos 1 fecha fin
		scrappingWork(fechas.get(0),fechas.get(1));
	}
	
	public static List<String> guiFechas() {
		List<String> listaFechas = new ArrayList<String> ();
		
		JTextField firstFecha = new JTextField(10);
		JTextField lastFecha = new JTextField(10);

		Object[] msg = {"Antes debe borrar el archivo csv en caso de buscar el mismo rango de fecha.\n\nPrimer fecha (DD/MM/YYYY):", firstFecha, "Última Fecha (DD/MM/YYYY):", lastFecha};

		Component frame = null;
		int result = JOptionPane.showConfirmDialog(
		    frame,
		    msg,
		    "Scrapping llamados vigentes by CF",
		    JOptionPane.OK_CANCEL_OPTION,
		    JOptionPane.PLAIN_MESSAGE);
		
		if (result == JOptionPane.YES_OPTION){
			if(verifyFecha(firstFecha.getText())==true && verifyFecha(lastFecha.getText())) {
				listaFechas.add(firstFecha.getText());
				listaFechas.add(lastFecha.getText());
			}else {
				JOptionPane.showMessageDialog(null, "Ingrese fechas con formato DD/MM/YYYY en la proxima ejecución.");
				System.exit(0);
			}
		}else {
			System.exit(0);
		}
		return listaFechas;
	}
	
	public static boolean verifyFecha (String fecha) {
		boolean resultado=true;
		for(int i=0;i<fecha.length();i++) {
			Character letra = fecha.charAt(i);
			if(letra.equals('/') || Character.isDigit(letra) ){
				resultado=true;
			}else {
				resultado=false;
				break;
			}
		}	
		return resultado;
	}
	
	public static void scrappingWork (String fechaInicio, String fechaFin) throws InterruptedException, IOException {
		//Inicializo driver
		WebDriver driver = initializeDriver (); 
		
		/*
		 * ---- STRING PATHS  ---- 
		 */
		String pHeader = "//div[@class='Header-top']"; //HEADER
		String pFooter = "//footer[@class='Footer']"; //FOOTER
		String qtyQuerys = "//div[@class='col-md-12' and contains(text(),'Se encontraron')] //strong"; // Al filtrar cuantos resultados se obtienen 
		
		/* FILTRO SECTION */
		String pFilterFechaTipo= "//div[@class='form-group'] //select[@name='tipo-fecha']";
		String pFilterFechaTipoPub = "//div[@class='form-group'] //select[@name='tipo-fecha'] //option[@value='PUB']";
		String pFilterFechaSelector = "//input[@id='rango-fecha']";
		String pFilterFechaSelectorIni = "//input[@name='daterangepicker_start']"; 
		String pFilterFechaSelectorFin = "//input[@name='daterangepicker_end']";
		String pFilterFechaAplicar = "//button[@class='applyBtn btn btn-small btn-sm btn-success']";
		String pFilterTotalAplicar ="//button[@class='btn btn-primary btn-block']";
		
		/* ROW ITEM */
		String pConCod = "(//div[@class='row item'] [$] //div[@class='col-md-12'] //div[@class='row'] //div[@class='col-md-5'] //h3 //a) [1]"; 
		String pConEnte = "(//div[@class='row item'] [$] //div[@class='col-md-12'] //div[@class='row'] //div[@class='col-md-5'] //h3 //a //span) [1]";
		String pConDesc = "(//div[@class='row item'] [$] //div[@class='col-md-12'] //div[@class='row desc-sniped'] //div[@class='col-md-12'] //p) [1]";
		String pConRecep = "(//div[@class='row item'] [$] //div[@class='col-md-12'] //div[@class='row desc-sniped'] //div[@class='col-md-12'] //strong) [1]";
		String pConPubli = "(//div[@class='row item'] [$] //div[@class='col-md-12'] //div[@class='row v-middle desc-sniped'] //div //span) [1]";
				
		/*
		 * ---- STRING KEYWORDS  ---- 
		 */
		List<String> keywords = cargarKeywords();
		
		/*
		 * ---- JAVASCRIPT EXECUTORS  ---- 
		 */
		JavascriptExecutor js = (JavascriptExecutor) driver;
		
		/*
		 * ---- WEBELEMENTS - WORK WITH FILTERS  ---- 
		 */
		WebElement webFilterFechaTipo = driver.findElement(By.xpath(pFilterFechaTipo));
		WebElement webFilterFechaTipoPub = driver.findElement(By.xpath(pFilterFechaTipoPub));
		
		WebElement webFilterFechaSelector = driver.findElement(By.xpath(pFilterFechaSelector));
		WebElement webFilterFechaSelectorIni = driver.findElement(By.xpath(pFilterFechaSelectorIni));
		WebElement webFilterFechaSelectorFin = driver.findElement(By.xpath( pFilterFechaSelectorFin ));
		WebElement webFilterFechaAplicar = driver.findElement(By.xpath(pFilterFechaAplicar));
		WebElement webFilterTotalAplicar = driver.findElement(By.xpath(pFilterTotalAplicar));
		
		/*
		 * ---- ACTIONS - WORK WITH FILTERS  ---- 
		 */
		//Se elige el tipo de busqueda
		webFilterFechaTipo.click();
		webFilterFechaTipoPub.click();
		
		//Se selecciona fecha inicio y fin
		webFilterFechaSelector.click();
		eraseTextFieldAndFill(webFilterFechaSelectorIni,fechaInicio);
		eraseTextFieldAndFill(webFilterFechaSelectorFin,fechaFin);
		webFilterFechaAplicar.click();
		
		//Confirmar filtro total
		webFilterTotalAplicar.click();
		Thread.sleep(300);
		
		/*
		 * ---- CARGAR MAS RESULTADOS HASTA LLEGAR AL ULTIMO  ---- 
		 */
		int finResultadosV1 = 0;
		int qtyQuerysInt = Integer.parseInt(driver.findElement(By.xpath(qtyQuerys)).getText()) + 1;
		List <WebElement> listaWebTemp = driver.findElements(By.xpath("//div[@class='row item']"));
		
		while(listaWebTemp.size()!=qtyQuerysInt) {
			cargaMasResultados(js);
			listaWebTemp = driver.findElements(By.xpath("//div[@class='row item']"));
		}
		
		/*
		while(finResultadosV1 == 0) {
			cargaMasResultados(js);
			finResultadosV1 = driver.findElements(By.xpath("//div[.='No existen más resultados.']")).size(); //reviso si obtengo advertencia de fin de resultados
			
		}*/
		
		/*
		 * ---- WEB ELEMENTS - ITEMROW  ---- 
		 */
		WebElement webConCod;
		WebElement webConEnte;
		WebElement webConDesc;
		WebElement webConRecep;
		WebElement webConPubli;
		
		List<String> listaInfo = new ArrayList<String>();
		String infoRow = "";
		
		// --- GENERO EL MAXIMO DE ITERACIONES PARA REVISAR ITEM ROWS---
		String maxS = driver.findElement(By.xpath(qtyQuerys)).getText();
		int max = Integer.parseInt(maxS);
		
		for (int i=1;i<=max;i++) {
			char charI=(char)i;
			String stringI = ""+ i;
			webConCod = driver.findElement(By.xpath( pConCod.replace("$", stringI) ));
			webConEnte = driver.findElement(By.xpath( pConEnte.replace("$", stringI) ));
			webConDesc = driver.findElement(By.xpath( pConDesc.replace("$", stringI) ));
			webConRecep = driver.findElement(By.xpath( pConRecep.replace("$", stringI) ));
			webConPubli = driver.findElement(By.xpath( pConPubli.replace("$", stringI) ));
			
			//Se obtiene el codigo concurso y se quita los espacios extras al final
			//String conCodigo = webConCod.getText().stripTrailing();
			String conCodigo = webConCod.getText(); // EXPERIMENTAL
			String strCodigo = conCodigo.substring(0, lastDigitStr(conCodigo));
			//strCodigo = strCodigo.stripTrailing().replace(",", ";");
			strCodigo = strCodigo.replace(",", ";");
			
			//String strEnte = webConEnte.getText().stripTrailing().replace(",", ";");
			String strEnte = webConEnte.getText().replace(",", ";"); //EXPERIMENTAL
			
			//String strRecepcion = webConRecep.getText().stripTrailing().replace(",", ";");
			String strRecepcion = webConRecep.getText().replace(",", ";"); //EXPERIMENTAL
			
			//String strPublicacion = webConPubli.getText().stripTrailing().replace(",", ";");
			String strPublicacion = webConPubli.getText().replace(",", ";"); //EXPERIMENTAL
			strPublicacion = strPublicacion.substring(11,strPublicacion.length());
			if(strPublicacion.indexOf('|') > 0) {
				strPublicacion = strPublicacion.substring(0, strPublicacion.indexOf('|')-1);
			}
			
			//String strDescripcion = webConDesc.getText().stripTrailing().replace(",", ";");
			String strDescripcion = webConDesc.getText().replace(",", ";"); //EXPERIMENTAL
			
			String linkLlamado = webConCod.getAttribute("href");
			
			infoRow = strCodigo+",";
			infoRow = infoRow + linkLlamado+",";
			infoRow = infoRow+strEnte+",";
			infoRow = infoRow+strRecepcion+",";
			infoRow = infoRow+strPublicacion+",";
			infoRow = infoRow+strDescripcion + ",";
			
			String check = infoRow.toLowerCase();
			
			if(inLista(infoRow,keywords)) {
				infoRow = infoRow + "Verdadero";
			}else {
				infoRow = infoRow + "Falso";
			}		
			listaInfo.add(infoRow);
			infoRow="";
		}//TERMINA FOR
		
		String fileName = "Llamados_"+fechaInicio.replace("/", "-")+"_"+fechaFin.replace("/", "-");
		escribirCSV(listaInfo,fileName);
		js.executeScript("print()");
		
		Thread.sleep(3000);
		savePDFPage(js);
		Thread.sleep(3000);
		driver.quit();
	}
	
	public static void savePDFPage (JavascriptExecutor js ) throws IOException {
		
		String relativePath = System.getProperty("user.dir")+"\\resources\\savePDFChromeScript.exe";
		Runtime.getRuntime().exec(relativePath); //Executes a script prepared in AutoIT
	}
		
	public static void escribirCSV (List<String> listaInfo, String fileName) {
		PrintWriter pw = null;
		String newFile = fileName + ".csv";
        try {
            //pw = new PrintWriter(new File("NewData.csv"));
        	pw = new PrintWriter(new File(newFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder builder = new StringBuilder();
        String columnNamesList = "Llamado,Link,Dependencia,Recepcion,Publicacion,Descripcion,Matchea_Keywords";
        // No need give the headers Like: id, Name on builder.append
        builder.append(columnNamesList +"\n");
        for(int i=0;i<listaInfo.size();i++) {
			//System.out.println(rowS);
        	builder.append(listaInfo.get(i));
            builder.append('\n');
		}
        pw.write(builder.toString());
        pw.close();
        //System.out.println("done!");
	}
	
	public static boolean inLista (String palabra, List<String> lista) {
		boolean result = false;
		
		for(String check : lista) {
			if (palabra.matches(check)) {
				result = true;
			}
		}
		
		return result;
	}
	
	public static int lastDigitStr (String sFix) {
		int lastDigit = -1;
		for(int j=0;j<sFix.length();j++) {
			if(Character.isDigit(sFix.charAt(j)) && j>lastDigit) {
				lastDigit = j;
			}
		}
		return lastDigit+1;
	}
	
	public static void cargaMasResultados(JavascriptExecutor js) throws InterruptedException {
		Thread.sleep(800);
		js.executeScript("window.scrollBy(5000,5000);"); //scroll down
		Thread.sleep(800);
		js.executeScript("window.scrollBy(1,1);");
		Thread.sleep(800);
	}
	
	public static void eraseTextFieldAndFill (WebElement textFieldEle, String insertar) {
		textFieldEle.sendKeys(Keys.chord(Keys.CONTROL,"a",Keys.DELETE));
		textFieldEle.sendKeys(insertar);
	}
	
	public static List<String> cargarKeywords (){
		List<String> keywords = new ArrayList<String> ();
		//Se recomienda que no coincida con "No existen más resultados."
		keywords.add("(.*)generador(.*)");
		keywords.add("(.*)electricidad(.*)");
		keywords.add("(.*)transformador(.*)");
		keywords.add("(.*)electricista(.*)");
		keywords.add("(.*)tension(.*)");
		keywords.add("(.*)tensión(.*)");
		keywords.add("(.*)ingenieria(.*)");
		keywords.add("(.*)ingeniería(.*)");
		keywords.add("(.*)ensayo(.*)");
		keywords.add("(.*)mantenimiento(.*)");
		keywords.add("(.*)medicion(.*)");
		keywords.add("(.*)medición(.*)");
		keywords.add("(.*)medi(.*)");
		keywords.add("(.*)montaje(.*)");
		keywords.add("(.*)diagnostico(.*)");
		keywords.add("(.*)diagnóstico(.*)");
		keywords.add("(.*)alta(.*)");
		keywords.add("(.*)media(.*)");
		keywords.add("(.*)pmi(.*)");
		keywords.add("(.*)supervi(.*)");
		keywords.add("(.*)gestión(.*)");
		keywords.add("(.*)gestion(.*)");
		keywords.add("(.*)tecnico(.*)");
		keywords.add("(.*)técnico(.*)");
		keywords.add("(.*)auditoria(.*)");
		keywords.add("(.*)auditoría(.*)");
		keywords.add("(.*)transmisión(.*)");
		keywords.add("(.*)transmision(.*)");
		keywords.add("(.*)generación(.*)");
		keywords.add("(.*)energía(.*)");
		keywords.add("(.*)energia(.*)");
		keywords.add("(.*)eléctrica(.*)");
		keywords.add("(.*)electrica(.*)");
		keywords.add("(.*)electri(.*)");
		keywords.add("(.*)subestacion(.*)");
		keywords.add("(.*)aerogenerador(.*)");
		keywords.add("(.*)aislación(.*)");
		keywords.add("(.*)aislacion(.*)");
		keywords.add("(.*)móvil(.*)");
		keywords.add("(.*)movil(.*)");
		keywords.add("(.*)kv(.*)");
		keywords.add("(.*)kw(.*)");
		keywords.add("(.*)fabrica(.*)");
		keywords.add("(.*)fábrica(.*)");
		keywords.add("(.*)fabricación(.*)");
		keywords.add("(.*)fabricacion(.*)");
		keywords.add("(.*)reparar(.*)");
		keywords.add("(.*)reparacion(.*)");
		keywords.add("(.*)reparación(.*)");
		keywords.add("(.*)equipamiento(.*)");
		keywords.add("(.*)desarrollo(.*)");
		keywords.add("(.*)obra(.*)");
		keywords.add("(.*)plano(.*)");
		keywords.add("(.*)control(.*)");
		keywords.add("(.*)comunicación(.*)");
		keywords.add("(.*)comunicacion(.*)");
		keywords.add("(.*)staff(.*)");
		
		return keywords;
	}
	
	public static WebDriver initializeDriver () {
		String relativePath = System.getProperty("user.dir")+"\\resources\\chromedriver.exe"; //fichero donde esta el ejecutable
		
		//Creo WebDriver
		System.setProperty("webdriver.chrome.driver", relativePath);
		
		ChromeOptions options = new ChromeOptions();
		options.addArguments("kiosk-printing"); //take out extra steps to print pdf page
		
		WebDriver driver = new ChromeDriver(options);
		driver.manage().window().maximize();
		
		//Accedo a link
		String url = "https://www.comprasestatales.gub.uy/consultas/";
		driver.get(url);
		
		return driver;
	}
	
	

	
}
