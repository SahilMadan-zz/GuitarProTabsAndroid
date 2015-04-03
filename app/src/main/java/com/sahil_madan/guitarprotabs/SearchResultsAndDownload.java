package com.sahil_madan.guitarprotabs;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SearchResultsAndDownload extends ActionBarActivity {

    private ArrayList<GPTab> tabs;

    private int current_page_ind = 1;
    private int last_page_ind = -1;

    private String search_input = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        tabs = new ArrayList<>();

        Intent intent = getIntent();
        search_input = intent.getStringExtra(StartSearch.EXTRA_SEARCH_INPUT);

        String url = getURL(search_input, 1);

        new GetUGTabsTask(this).execute(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getURL(String search_input, int page)
    {
        String processed_input = "";
        boolean prev_space = false;
        for (int i = 0; i < search_input.length(); ++i) {
            Character c = search_input.charAt(i);
            if (Character.isLetter(c)) {
                processed_input += Character.toLowerCase(c);
                prev_space = false;
            } else if (Character.isDigit(c)) {
                processed_input += c;
                prev_space = false;
            } else if ((Character.isSpaceChar(c) || c.equals('\n')) && !prev_space) {
                processed_input += '+';
                prev_space = true;
            } else if (!(Character.isSpaceChar(c) || c.equals('\n'))) {
                prev_space = false;
            }
        }

        return "http://www.ultimate-guitar.com/search.php?title="
        + processed_input
        + "&page="
        + Integer.toString(page)
        +"&tab_type_group=text&app_name=ugt&type=500&order=title_srt";
    }

    public void downloadTab(View view)
    {
        int position = (int) view.getTag();

        DownloadUGTabTask download_task = new DownloadUGTabTask(this);
        download_task.execute(tabs.get(position).link);
    }

    public void jumpFirstPage(View view)
    {
        String url = getURL(search_input, 1);
        current_page_ind = 1;
        new GetUGTabsTask(this).execute(url);
    }

    public void jumpPrevPage(View view)
    {
        String url = getURL(search_input, current_page_ind - 1);
        current_page_ind -= 1;
        new GetUGTabsTask(this).execute(url);
    }

    public void jumpNextPage(View view)
    {
        String url = getURL(search_input, current_page_ind + 1);
        current_page_ind += 1;
        new GetUGTabsTask(this).execute(url);
    }

    public void jumpLastPage(View view)
    {
        String url = getURL(search_input, last_page_ind);
        current_page_ind = last_page_ind;
        new GetUGTabsTask(this).execute(url);
    }

    private class GetUGTabsTask extends AsyncTask<String, Void, Document> {

        Activity activity = null;
        TextView search_notabs = null;
        ProgressBar search_progress = null;
        ListView search_results = null;
        LinearLayout page_buttons = null;
        Button first_page = null;
        Button prev_page = null;
        Button next_page = null;
        Button last_page = null;

        public GetUGTabsTask(Activity activity)
        {
            this.activity = activity;
            search_progress = (ProgressBar) this.activity.findViewById(R.id.gpsearch_progress);
            search_notabs = (TextView) this.activity.findViewById(R.id.gpsearch_notabs);
            search_results = (ListView) this.activity.findViewById(R.id.gptablist);
            page_buttons = (LinearLayout) this.activity.findViewById(R.id.gp_search_page_buttons);
            first_page = (Button) this.activity.findViewById(R.id.gp_search_page_first);
            prev_page = (Button) this.activity.findViewById(R.id.gp_search_page_prev);
            next_page = (Button) this.activity.findViewById(R.id.gp_search_page_next);
            last_page = (Button) this.activity.findViewById(R.id.gp_search_page_last);
        }

        @Override
        protected void onPreExecute()
        {
            search_progress.setVisibility(View.VISIBLE);
            search_notabs.setVisibility(View.INVISIBLE);
            search_results.setVisibility(View.INVISIBLE);
            page_buttons.setVisibility(View.GONE);
        }

        @Override
        protected Document doInBackground(String... urls) {
            try {
                Connection.Response res = Jsoup.connect(urls[0])
                        .userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
                        .execute();

                if (res == null || res.statusCode() != 200) {
                    return null;
                }

                Document doc = res.parse();
                if (doc == null) {
                    return null;
                }

                return doc;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Document doc) {
            if (doc == null) {
                search_progress.setVisibility(View.INVISIBLE);
                search_notabs.setVisibility(View.VISIBLE);
                search_results.setVisibility(View.INVISIBLE);
                page_buttons.setVisibility(View.GONE);
                return;
            }
            // Create TabList Items
            tabs.clear();

            String current_artist = "";
            Elements elements = doc.select("a.song, span.r_1, span.r_2, span.r_3, span.r_4, span.r_5, b.ratdig");
            boolean obtain_rating = false;
            boolean obtain_votes = false;
            for (Element em : elements) {
                // Skip Tab Pro tabs
                if (em.hasClass("js-tp_link")) {
                    obtain_rating = false;
                    obtain_votes = false;
                    continue;
                }

                // If New Artist Found
                if (em.hasClass("search_art")) {
                    current_artist = em.text();
                    continue;
                }

                // If new Tab Found
                if (em.hasClass("song")) {
                    obtain_rating = true;
                    obtain_votes = true;
                    String link = em.attr("href");
                    GPTab tab = new GPTab(current_artist, em.text(), -1, -1, link);
                    tabs.add(tab);
                    continue;
                }

                if (obtain_rating) {
                    if (em.hasClass("r_1")) {
                        tabs.get(tabs.size() - 1).rating = 1;
                        obtain_rating = false;
                        continue;
                    } else if (em.hasClass("r_2")) {
                        tabs.get(tabs.size() - 1).rating = 2;
                        obtain_rating = false;
                        continue;
                    } else if (em.hasClass("r_3")) {
                        tabs.get(tabs.size() - 1).rating = 3;
                        obtain_rating = false;
                        continue;
                    } else if (em.hasClass("r_4")) {
                        tabs.get(tabs.size() - 1).rating = 4;
                        obtain_rating = false;
                        continue;
                    } else if (em.hasClass("r_5")) {
                        tabs.get(tabs.size() - 1).rating = 5;
                        obtain_rating = false;
                        continue;
                    }
                }

                if (obtain_votes && em.hasClass("ratdig")) {
                    tabs.get(tabs.size() - 1).votes = Integer.parseInt(em.text());
                    obtain_votes = false;
                }
            }

            // Update Layout
            if (tabs.size() == 0) {
                search_progress.setVisibility(View.INVISIBLE);
                search_notabs.setVisibility(View.VISIBLE);
                search_results.setVisibility(View.INVISIBLE);
                page_buttons.setVisibility(View.GONE);
                return;
            }
            search_progress.setVisibility(View.INVISIBLE);
            search_notabs.setVisibility(View.INVISIBLE);
            search_results.setVisibility(View.VISIBLE);
            page_buttons.setVisibility(View.VISIBLE);

            GPTabAdapter adapter = new GPTabAdapter(this.activity, tabs);
            ListView gptablist = (ListView) findViewById(R.id.gptablist);
            gptablist.setAdapter(adapter);

            // Get the index of the last page (assume already obtained if current_page != 1)
            if (current_page_ind == 1) {
                Elements page_details = doc.select("div.paging a");
                last_page_ind = -1;
                for (Element pg : page_details) {
                    if (pg.text().equals("Next") || pg.text().equals("Prev")) {
                        continue;
                    }
                    int value = Integer.parseInt(pg.text());
                    if (value > last_page_ind) {
                        last_page_ind = value;
                    }
                }
            }

            // Enable Jump-Page buttons
            if (last_page_ind == -1) {
                first_page.setEnabled(false);
                prev_page.setEnabled(false);
                last_page.setEnabled(false);
                next_page.setEnabled(false);
            }

            if (current_page_ind > 2) {
                first_page.setEnabled(true);
                prev_page.setEnabled(true);
            } else if (current_page_ind == 2) {
                first_page.setEnabled(false);
                prev_page.setEnabled(true);
            } else {
                first_page.setEnabled(false);
                prev_page.setEnabled(false);
            }

            if (current_page_ind < (last_page_ind - 1)) {
                last_page.setEnabled(true);
                next_page.setEnabled(true);
            } else if (current_page_ind == (last_page_ind - 1)) {
                last_page.setEnabled(false);
                next_page.setEnabled(true);
            } else {
                last_page.setEnabled(false);
                next_page.setEnabled(false);
            }
        }
    }

    private class DownloadUGTabTask extends AsyncTask<String, Void, Connection.Response> {

        Activity activity = null;

        public DownloadUGTabTask(Activity activity)
        {
            this.activity = activity;
        }

        protected void onPreExecute()
        {
            Toast.makeText(activity, "Downloading tab", Toast.LENGTH_SHORT).show();
        }

        private String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
        private String referrer = "http://www.google.com";

        protected Connection.Response doInBackground(String... urls)
        {
            try {
                Connection.Response res = Jsoup.connect(urls[0])
                        .userAgent(userAgent)
                        .referrer(referrer)
                        .execute();

                Document doc = res.parse();
                Map<String, String> cookies = res.cookies();

                // Scrape page for download form
                String download_url = "http://tabs.ultimate-guitar.com/tabs/download?";
                Elements form_elements = doc.select("form.tab_downloadcl");
                Elements elements = form_elements.select("input[name]");
                for (Element em : elements) {
                    if (em.attr("name").equals("id")) {
                        download_url += "id=" + em.attr("value") + "?";
                    }

                    if (em.attr("name").equals("session_id")) {
                        download_url += "session_id=" + em.attr("value");
                    }
                }

                // Download tab
                res = Jsoup.connect(download_url)
                        .userAgent(userAgent)
                        .cookies(cookies)
                        .ignoreContentType(true)
                        .maxBodySize(2000000)
                        .execute();

                return res;

            } catch (IOException e) {
                return null;
            }
        }

        protected void onPostExecute(Connection.Response res)
        {
            // Check for network errors
            if (res == null || res.statusCode() != 200) {
                String failed = activity.getResources().getString(R.string.tab_download_unsuccessful);
                Toast.makeText(activity, failed, Toast.LENGTH_SHORT).show();
                return;
            }

            // Get Data
            byte[] data = res.bodyAsBytes();

            // Get filename from header
            String content_dis = res.header("content-disposition");
            Pattern filename_rgx = Pattern.compile("(?<=filename=\").*?(?=\")");
            Matcher rgx_matcher = filename_rgx.matcher(content_dis);
            String filename = null;
            if (rgx_matcher.find()) {
                filename = rgx_matcher.group();
            }

            // Save File
            String saved_filename = saveGPBytesToFile(data, filename);
            if (saved_filename != null) {
                String downloaded = activity.getResources().getString(R.string.tab_save_successful);
                Toast.makeText(activity, String.format(downloaded, saved_filename), Toast.LENGTH_SHORT).show();
            } else {
                String failed = activity.getResources().getString(R.string.tab_save_unsuccessful);
                Toast.makeText(activity, failed, Toast.LENGTH_SHORT).show();
            }
        }

        private String saveGPBytesToFile(byte[] data, String filename)
        {
            // Create file in download directory
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            int counter = 1;
            while (file.exists() && counter < 100) {
                int i = filename.lastIndexOf(".");
                String filename_fixed = filename.substring(0, i) + " (" + counter + ")." + filename.substring(i + 1, filename.length());
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename_fixed);
                counter++;
            }

            if (file.exists()) {
                return null;
            }

            // Save data into file
            try {
                file.createNewFile();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bos.write(data);
                bos.flush();
                bos.close();
            } catch (IOException e) {
                return null;
            }

            return file.getName();
        }
    }
}
