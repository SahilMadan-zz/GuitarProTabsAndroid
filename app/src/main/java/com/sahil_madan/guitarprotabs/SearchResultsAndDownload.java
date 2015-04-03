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
    private static String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";
    private String searchQuery = null;
    private int currentPageIndex = 1;
    private int lastPageIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        tabs = new ArrayList<>();

        Intent intent = getIntent();
        searchQuery = processQueryForSearch(intent.getStringExtra(StartSearch.EXTRA_SEARCH_INPUT));

        new GetUGTabsTask(this).execute(getURL());
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

    /* Return a Ultimate Guitar URL */
    private String getURL()
    {
        String searchURL = "http://www.ultimate-guitar.com/search.php?title=%1$s&page=%2$d&tab_type_group=text&app_name=ugt&type=500&order=title_srt";
        return String.format(searchURL, searchQuery, currentPageIndex);
    }


    private String processQueryForSearch(String rawQuery)
    {
        String processedQuery = "";
        boolean prev_space = false;
        for (int i = 0; i < rawQuery.length(); ++i) {
            Character c = rawQuery.charAt(i);
            if (Character.isLetter(c)) {
                processedQuery += Character.toLowerCase(c);
                prev_space = false;
            } else if (Character.isDigit(c)) {
                processedQuery += c;
                prev_space = false;
            } else if ((Character.isSpaceChar(c) || c.equals('\n')) && !prev_space) {
                processedQuery += '+';
                prev_space = true;
            } else if (!(Character.isSpaceChar(c) || c.equals('\n'))) {
                prev_space = false;
            }
        }

        return processedQuery;
    }

    public void downloadTab(View view)
    {
        int position = (int) view.getTag();

        DownloadUGTabTask download_task = new DownloadUGTabTask(this);
        download_task.execute(tabs.get(position).link);
    }

    public void jumpFirstPage(View view)
    {
        currentPageIndex = 1;
        String url = getURL();
        new GetUGTabsTask(this).execute(url);
    }

    public void jumpPrevPage(View view)
    {
        currentPageIndex -= 1;
        String url = getURL();
        new GetUGTabsTask(this).execute(url);
    }

    public void jumpNextPage(View view)
    {
        currentPageIndex += 1;
        String url = getURL();
        new GetUGTabsTask(this).execute(url);
    }

    public void jumpLastPage(View view)
    {
        currentPageIndex = lastPageIndex;
        String url = getURL();
        new GetUGTabsTask(this).execute(url);
    }

    /* Task searches UG and gets tab results, updating the layout in the process */
    private class GetUGTabsTask extends AsyncTask<String, Void, Document> {

        Activity activity = null;
        TextView noTabsText = null;
        ProgressBar searchProgress = null;
        ListView searchResultsList = null;
        LinearLayout pageButtonsLayout = null;
        Button firstPageButton = null;
        Button prevPageButton = null;
        Button nextPageButton = null;
        Button lastPageButton = null;

        public GetUGTabsTask(Activity activity)
        {
            this.activity = activity;
            searchProgress = (ProgressBar) this.activity.findViewById(R.id.gpsearch_progress);
            noTabsText = (TextView) this.activity.findViewById(R.id.gpsearch_notabs);
            searchResultsList = (ListView) this.activity.findViewById(R.id.gptablist);
            pageButtonsLayout = (LinearLayout) this.activity.findViewById(R.id.gp_search_page_buttons);
            firstPageButton = (Button) this.activity.findViewById(R.id.gp_search_page_first);
            prevPageButton = (Button) this.activity.findViewById(R.id.gp_search_page_prev);
            nextPageButton = (Button) this.activity.findViewById(R.id.gp_search_page_next);
            lastPageButton = (Button) this.activity.findViewById(R.id.gp_search_page_last);
        }

        @Override
        protected void onPreExecute()
        {
            searchProgress.setVisibility(View.VISIBLE);
            noTabsText.setVisibility(View.INVISIBLE);
            searchResultsList.setVisibility(View.INVISIBLE);
            pageButtonsLayout.setVisibility(View.GONE);
        }

        @Override
        protected Document doInBackground(String... urls) {
            try {
                Connection.Response res = Jsoup.connect(urls[0])
                        .userAgent(userAgent)
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
            // If doc returns null, there was a network error of some sort
            if (doc == null) {
                searchProgress.setVisibility(View.INVISIBLE);
                noTabsText.setVisibility(View.VISIBLE);
                searchResultsList.setVisibility(View.INVISIBLE);
                pageButtonsLayout.setVisibility(View.GONE);
                Toast.makeText(activity, activity.getString(R.string.gpsearch_networkerror), Toast.LENGTH_SHORT).show();
                return;
            }

            // Create TabList Items
            tabs.clear();

            // Scrape page for tabs
            String currentArtist = "";
            Elements elements = doc.select("a.song, span.r_1, span.r_2, span.r_3, span.r_4, span.r_5, b.ratdig");
            boolean obtainRating = false;
            boolean obtainVotes = false;
            for (Element em : elements) {
                // Skip Tab Pro tabs
                if (em.hasClass("js-tp_link")) {
                    obtainRating = false;
                    obtainVotes = false;
                    continue;
                }

                // If New Artist Found
                if (em.hasClass("search_art")) {
                    currentArtist = em.text();
                    continue;
                }

                // If new Tab Found
                if (em.hasClass("song")) {
                    obtainRating = true;
                    obtainVotes = true;
                    String link = em.attr("href");
                    GPTab tab = new GPTab(currentArtist, em.text(), -1, -1, link);
                    tabs.add(tab);
                    continue;
                }

                if (obtainRating) {
                    if (em.hasClass("r_1")) {
                        tabs.get(tabs.size() - 1).rating = 1;
                        obtainRating = false;
                        continue;
                    } else if (em.hasClass("r_2")) {
                        tabs.get(tabs.size() - 1).rating = 2;
                        obtainRating = false;
                        continue;
                    } else if (em.hasClass("r_3")) {
                        tabs.get(tabs.size() - 1).rating = 3;
                        obtainRating = false;
                        continue;
                    } else if (em.hasClass("r_4")) {
                        tabs.get(tabs.size() - 1).rating = 4;
                        obtainRating = false;
                        continue;
                    } else if (em.hasClass("r_5")) {
                        tabs.get(tabs.size() - 1).rating = 5;
                        obtainRating = false;
                        continue;
                    }
                }

                if (obtainVotes && em.hasClass("ratdig")) {
                    tabs.get(tabs.size() - 1).votes = Integer.parseInt(em.text());
                    obtainVotes = false;
                }
            }

            // Update layout where no tabs found
            if (tabs.size() == 0) {
                searchProgress.setVisibility(View.INVISIBLE);
                noTabsText.setVisibility(View.VISIBLE);
                searchResultsList.setVisibility(View.INVISIBLE);
                pageButtonsLayout.setVisibility(View.GONE);
                return;
            }

            // Update layout where tabs found
            searchProgress.setVisibility(View.INVISIBLE);
            noTabsText.setVisibility(View.INVISIBLE);
            searchResultsList.setVisibility(View.VISIBLE);
            pageButtonsLayout.setVisibility(View.VISIBLE);

            GPTabAdapter adapter = new GPTabAdapter(this.activity, tabs);
            ListView gptablist = (ListView) findViewById(R.id.gptablist);
            gptablist.setAdapter(adapter);

            // Get the index of the last page (assume already obtained if lastPage != -1)
            if (lastPageIndex == -1) {
                Elements page_details = doc.select("div.paging a");
                lastPageIndex = -1;
                for (Element pg : page_details) {
                    if (pg.text().equals("Next") || pg.text().equals("Prev")) {
                        continue;
                    }
                    int value = Integer.parseInt(pg.text());
                    if (value > lastPageIndex) {
                        lastPageIndex = value;
                    }
                }
            }

            // Enable Jump-Page buttons
            if (lastPageIndex == -1) {
                firstPageButton.setEnabled(false);
                prevPageButton.setEnabled(false);
                lastPageButton.setEnabled(false);
                nextPageButton.setEnabled(false);
            }

            if (currentPageIndex > 2) {
                firstPageButton.setEnabled(true);
                prevPageButton.setEnabled(true);
            } else if (currentPageIndex == 2) {
                firstPageButton.setEnabled(false);
                prevPageButton.setEnabled(true);
            } else {
                firstPageButton.setEnabled(false);
                prevPageButton.setEnabled(false);
            }

            if (currentPageIndex < (lastPageIndex - 1)) {
                lastPageButton.setEnabled(true);
                nextPageButton.setEnabled(true);
            } else if (currentPageIndex == (lastPageIndex - 1)) {
                lastPageButton.setEnabled(false);
                nextPageButton.setEnabled(true);
            } else {
                lastPageButton.setEnabled(false);
                nextPageButton.setEnabled(false);
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
            Toast.makeText(activity, activity.getString(R.string.tab_downloading), Toast.LENGTH_SHORT).show();
        }

        protected Connection.Response doInBackground(String... urls)
        {
            try {
                Connection.Response res = Jsoup.connect(urls[0])
                        .userAgent(userAgent)
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
                if (!file.createNewFile()) {
                    return null;
                }
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
