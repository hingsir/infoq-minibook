package infoqcn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author mynacche
 * @date 2013-7-18
 */
public class MiniBook {

	private static final String DOMAIN = "http://www.infoq.com";
	private static final String DEFAULT_URL = "http://www.infoq.com/cn/minibooks/";
	private static final int PAGE_SIZE = 12;
	private static final String DOWNLOAD_FOLDER = "E:\\minibooks\\";

	private static final String USERNAME = "cheche0917@126.com";
	private static final String PASSWORD = "cxy0917";

	private static String cookie = null;

	public static List<String> getBookUrls() {

		List<String> book_urls = new ArrayList<String>();
		Document doc = null;
		Elements h2s = null;
		int index = 0;
		try {
			while (index == 0 || h2s.size() > 0) {

				doc = Jsoup.connect(DEFAULT_URL + index).timeout(60000).get();

				h2s = doc.select("h2[class*=itemtitle]");
				for (Element h2 : h2s) {
					book_urls.add(DOMAIN + h2.child(0).attr("href"));
				}
				index += PAGE_SIZE;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return book_urls;
	}

	public static String getDownloadLink(String url) {

		String download_link = null;
		Document doc;
		try {
			doc = Jsoup.connect(url).timeout(60000).get();

			Elements spans = doc.select("span[class*=downloadLinks]");
			String onclick_str = null;
			for (Element span : spans) {
				onclick_str = span
						.child(0)
						.attr("onclick")
						.replace("$('#dldLinkFinal').val('", "")
						.replace("');UserActions_Login.showLoginWidget(this,'minibookDownload');",
								"");
				download_link = DOMAIN + onclick_str;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return download_link;
	}

	public static boolean download(String urlString) {

		boolean flag = false;

		String[] arr = urlString.split("/");
		String filename = arr[arr.length - 1];
		filename = DOWNLOAD_FOLDER + filename;
		if (new File(filename).exists()) {
			return true;
		}
		try {

			if (cookie == null) {
				cookie = login(USERNAME, PASSWORD);
				if (cookie == null) {
					return false;
				} else {
					System.out.println("login success,download started :)");
				}
			}

			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Cookie", cookie);
			conn.connect();
			InputStream is = conn.getInputStream();
			OutputStream os = new FileOutputStream(filename);
			byte[] b = new byte[1024];
			int len = -1;
			while ((len = is.read(b)) != -1) {
				os.write(b, 0, len);
			}
			os.flush();
			is.close();
			os.close();

			flag = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return flag;
	}

	public static String login(String username, String password) {

		String ret = null;

		String loginUrl = "https://www.infoq.com/cn/login.action?" + "username=" + username
				+ "&password=" + password;

		try {
			URL url = new URL(loginUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			/*
			 * conn.setDoOutput(true); OutputStreamWriter out = new
			 * OutputStreamWriter(conn.getOutputStream(), "utf-8");
			 * out.write("username=" + username + "&password=" + password);
			 * out.flush(); out.close(); ret =
			 * conn.getHeaderField("Set-Cookie"); System.out.println(ret);
			 */
			Map<String, List<String>> map = conn.getHeaderFields();
			List<String> list = map.get("Set-Cookie");

			for (String s : list) {
				if (s.contains("UserCookie")) {
					int n = s.indexOf(';');
					ret = s.substring(0, n);
					break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return ret;
	}

	public static void main(String[] args) {

		System.out.println("Getting minibook urls started...");
		List<String> book_urls = getBookUrls();
		if (book_urls == null) {
			System.out.println("Getting minibook urls failed.");
			return;
		}
		System.out.println("Getting minibook urls success.");
		System.out.println("Total minibooks : " + book_urls.size());

		File file = new File(DOWNLOAD_FOLDER);
		if (!file.exists()) {
			file.mkdir();
		}
		int suc = 0;
		int fail = 0;
		for (String url : book_urls) {

			boolean b = false;
			System.out.println("getDownloadLink ...  [" + url + "]");
			String download_url = getDownloadLink(url);
			if (download_url != null) {
				System.out.println("download started : " + download_url);
				b = download(download_url);
				System.out.println("download " + (b ? "success" : "failed") + ": " + download_url);
			} else {
				System.out.println("getDownloadLink failed: [" + url + "]");
				b = false;
			}

			if (b) {
				suc++;
			} else {
				fail++;
			}
		}
		System.out.println("download completed: " + suc + " success," + fail + " failed.");

	}
}
