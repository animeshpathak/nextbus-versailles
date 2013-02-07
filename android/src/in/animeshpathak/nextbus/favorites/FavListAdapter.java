package in.animeshpathak.nextbus.favorites;

import in.animeshpathak.nextbus.R;
import in.animeshpathak.nextbus.favorites.FavoriteDialog.OnFavoriteSelectedListener;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class FavListAdapter extends BaseAdapter {

	private Context context;
	private List<Favorite> favorites;
	private OnFavoriteSelectedListener listener;

	public FavListAdapter(Context c, List<Favorite> favs,
			OnFavoriteSelectedListener listener) {
		this.context = c;
		this.favorites = favs;
		this.listener = listener;
	}

	@Override
	public int getCount() {
		return favorites.size();
	}

	@Override
	public Object getItem(int arg0) {
		return favorites.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(final int position, View view, ViewGroup group) {
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.fav_list_item, null);
		}

		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.favoriteSelected(favorites.get(position));
				}
			}
		});

		TextView txtType = (TextView) view.findViewById(R.id.textFavLine);
		txtType.setText(favorites.get(position).getLine());

		TextView streamId = (TextView) view.findViewById(R.id.textFavStop);
		streamId.setText(favorites.get(position).getStop());

		ImageButton removeFavButton = (ImageButton) view
				.findViewById(R.id.favorites_remove_button);
		removeFavButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				favorites.remove(position);
				FavListAdapter.this.notifyDataSetChanged();
			}
		});

		return view;
	}

}
