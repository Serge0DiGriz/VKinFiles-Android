package di_griz.vkinfiles;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MyAdapter extends FragmentPagerAdapter {

    MyAdapter(FragmentManager mgr) {
        super(mgr);
    }

    @Override
    public int getCount() {
        return (3);
    }

    @Override
    public Fragment getItem(int position) {
        return (PageFragment.newInstance(position));
    }

    @Override
    public String getPageTitle(int position) {
        return (PageFragment.getTitle(position));
    }
}
