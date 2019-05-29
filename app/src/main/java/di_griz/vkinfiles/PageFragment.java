package di_griz.vkinfiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

public class PageFragment extends Fragment {
    private int pageNumber;

    public static PageFragment newInstance(int page) {
        PageFragment fragment = new PageFragment();
        Bundle args=new Bundle();
        args.putInt("num", page);
        fragment.setArguments(args);
        return fragment;
    }

    public PageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments() != null ? getArguments().getInt("num") : 1;
    }

    static String getTitle(int position) {
        String title;
        switch (position) {
            case 0: title = "Photos"; break;
            case 1: title = "Audios"; break;
            case 2: title = "Documents"; break;
            default: title = "Empty";
        }

        return title;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result=inflater.inflate(R.layout.fragment_page, container, false);
        ListAdapter adapter;
        switch (pageNumber) {
            case 0: adapter = MainActivity.photoAdapter; break;
            case 1: adapter = MainActivity.audioAdapter; break;
            case 2: adapter = MainActivity.docAdapter; break;
            default: adapter = MainActivity.emptyAdapter;
        }
        ((ListView)result.findViewById(R.id.listView)).setAdapter(adapter);

        return result;
    }

}
