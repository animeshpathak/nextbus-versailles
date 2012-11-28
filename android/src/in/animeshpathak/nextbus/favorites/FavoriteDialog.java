package in.animeshpathak.nextbus.favorites;

import in.animeshpathak.nextbus.Constants;
import in.animeshpathak.nextbus.R;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

public class FavoriteDialog {
	private List<Favorite> favList;
	private FavListAdapter favAdapter;
	private AlertDialog dialog;

	public FavoriteDialog(final Activity context,
			final OnFavoriteSelectedListener listener, final String curLine,
			final String curStop) {

		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			favList = Favorite.readFavorites(context);
			favAdapter = new FavListAdapter(context, favList,
					new OnFavoriteSelectedListener() {
						@Override
						public void favoriteSelected(Favorite fav) {
							listener.favoriteSelected(fav);
							dialog.cancel();
						}
					});
			builder.setAdapter(favAdapter, null);

			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View titleView = inflater.inflate(R.layout.fav_list_title, null);
			ImageButton addToFavButton = (ImageButton) titleView
					.findViewById(R.id.favorites_add_button);
			addToFavButton
					.setOnClickListener(new android.view.View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Favorite fav = new Favorite(curLine, curStop);
							if (!favList.contains(fav)) {
								favList.add(fav);
								favAdapter.notifyDataSetChanged();
							}
						}
					});
			builder.setCustomTitle(titleView);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					try {
						Favorite.saveFavorites(context, favList);
					} catch (IOException e) {
						Log.e(Constants.LOG_TAG, e.getMessage(), e);
					}
				}
			});
			dialog = builder.create();
			dialog.show();
		} catch (Exception e) {
			Log.e(Constants.LOG_TAG, e.getMessage(), e);
		}
	}

	public interface OnFavoriteSelectedListener {
		public void favoriteSelected(Favorite fav);
	}
}
