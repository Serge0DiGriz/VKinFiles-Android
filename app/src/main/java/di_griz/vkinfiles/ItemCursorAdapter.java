package di_griz.vkinfiles;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class ItemCursorAdapter extends CursorAdapter {

    static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

    ItemCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater =  (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.item,null,true);

        ViewHolder holder = new ViewHolder();
        holder.textView = rowView.findViewById(R.id.itemText);
        holder.imageView = rowView.findViewById(R.id.itemImage);
        rowView.setTag(holder);

        return rowView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.textView.setText(cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_TITLE)));

        String type = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_TYPE));
        Drawable draw;

        try {
            if (type.equals("audio"))
                draw = Drawable.createFromStream(context.getAssets()
                        .open("audio.png"), null);
            else if (type.equals("link"))
                draw = Drawable.createFromStream(context.getAssets()
                        .open("folder_audio.png"), null);
            else if (type.equals("doc"))
                draw = Drawable.createFromStream(context.getAssets()
                        .open("document.png"), null);
            else if (type.equals("photo"))
                draw = Drawable.createFromStream(context.getAssets()
                        .open("image.png"), null);
            else
                draw = context.getResources().getDrawable(R.mipmap.ic_launcher);
        } catch (IOException error) {
            draw = context.getResources().getDrawable(R.mipmap.ic_launcher);
        }
        holder.imageView.setImageDrawable(draw);
    }

}
