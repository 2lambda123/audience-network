/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.AdUnitsSample.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.facebook.samples.AdUnitsSample.R;
import java.util.ArrayList;
import java.util.List;

public class NativeBannerAdFragment extends Fragment implements NativeAdListener {
  public static final String USE_IMAGE_VIEW = "useImageView";

  private static final String TAG = NativeBannerAdFragment.class.getSimpleName();

  private LinearLayout mAdView;
  private FrameLayout mAdChoicesContainer;
  private NativeAdLayout mNativeBannerAdContainer;
  private @Nullable NativeBannerAd mNativeBannerAd;

  private TextView mNativeBannerAdStatusLabel;

  private boolean isAdViewAdded;

  private boolean mUseImageView;

  public static NativeBannerAdFragment newInstance(boolean useImageView) {
    NativeBannerAdFragment myFragment = new NativeBannerAdFragment();

    Bundle args = new Bundle();
    args.putBoolean(USE_IMAGE_VIEW, useImageView);
    myFragment.setArguments(args);

    return myFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUseImageView = arguments.getBoolean(USE_IMAGE_VIEW, false);
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_native_banner_ad, container, false);
    mNativeBannerAdContainer = view.findViewById(R.id.native_banner_ad_container);
    mNativeBannerAdStatusLabel = view.findViewById(R.id.native_banner_status_label);

    mAdView =
        (LinearLayout)
            inflater.inflate(R.layout.native_banner_ad_unit, mNativeBannerAdContainer, false);

    mAdChoicesContainer = mAdView.findViewById(R.id.ad_choices_container);

    Button showNativeBannerAdButton = view.findViewById(R.id.refresh_native_banner_button);
    showNativeBannerAdButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mNativeBannerAdStatusLabel.setText(getString(R.string.loading_status));

            // Create a native ad request with a unique placement ID (generate your own on the
            // Facebook app settings). Use different ID for each ad placement in your app.
            mNativeBannerAd = new NativeBannerAd(getContext(), "YOUR_PLACEMENT_ID");

            // When testing on a device, add its hashed ID to force test ads.
            // The hash ID is printed to log cat when running on a device and loading an ad.
            // AdSettings.addTestDevice("THE HASHED ID AS PRINTED TO LOG CAT");

            // Initiate a request to load an ad.
            mNativeBannerAd.loadAd(
                mNativeBannerAd
                    .buildLoadAdConfig()
                    .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
                    .withAdListener(NativeBannerAdFragment.this)
                    .build());
          }
        });
    //  load the Native Banner when this fragment is created
    //  as the Banner in BannerFragment does.
    showNativeBannerAdButton.performClick();

    return view;
  }

  @Override
  public void onError(Ad ad, AdError error) {
    mNativeBannerAdStatusLabel.setText("Ad Failed to Load: " + error.getErrorMessage());
  }

  @Override
  public void onAdLoaded(Ad ad) {
    if (mNativeBannerAd == null || mNativeBannerAd != ad) {
      // Race condition, load() called again before last ad was displayed
      return;
    }
    if (!isAdViewAdded) {
      isAdViewAdded = true;
      mNativeBannerAdContainer.addView(mAdView);
    }
    // Unregister last ad
    mNativeBannerAd.unregisterView();

    mNativeBannerAdStatusLabel.setText("");

    if (!mNativeBannerAd.isAdLoaded() || mNativeBannerAd.isAdInvalidated()) {
      mNativeBannerAdStatusLabel.setText("Ad is not loaded or invalidated.");
      return;
    }

    // Using the AdOptionsView is optional, but your native ad unit should
    // be clearly delineated from the rest of your app content. See
    // https://developers.facebook.com/docs/audience-network/guidelines/native-ads#native
    // for details. We recommend using the AdOptionsView.
    AdOptionsView adOptionsView =
        new AdOptionsView(
            getActivity(),
            mNativeBannerAd,
            mNativeBannerAdContainer,
            AdOptionsView.Orientation.HORIZONTAL,
            20);
    mAdChoicesContainer.removeAllViews();
    mAdChoicesContainer.addView(adOptionsView);

    inflateAd(mNativeBannerAd, mAdView);

    // Registering a touch listener to log which ad component receives the touch event.
    // We always return false from onTouch so that we don't swallow the touch event (which
    // would prevent click events from reaching the NativeAd control).
    // The touch listener could be used to do animations.
    mNativeBannerAd.setOnTouchListener(
        new View.OnTouchListener() {
          @Override
          public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
              int i = view.getId();
              if (i == R.id.native_ad_call_to_action) {
                Log.d(TAG, "Call to action button clicked");
              } else if (i == R.id.native_icon_view) {
                Log.d(TAG, "Main image clicked");
              } else {
                Log.d(TAG, "Other ad component clicked");
              }
            }
            return false;
          }
        });
  }

  @Override
  public void onAdClicked(Ad ad) {
    Toast.makeText(getActivity(), "Ad Clicked", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    Log.d(TAG, "onLoggingImpression");
  }

  @Override
  public void onMediaDownloaded(Ad ad) {
    Log.d(TAG, "onMediaDownloaded");
  }

  private void inflateAd(NativeBannerAd nativeBannerAd, View adView) {
    // Create native UI using the ad metadata.
    TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
    TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
    TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
    Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

    // Setting the Text
    nativeAdCallToAction.setText(nativeBannerAd.getAdCallToAction());
    nativeAdCallToAction.setVisibility(
        nativeBannerAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
    nativeAdTitle.setText(nativeBannerAd.getAdvertiserName());
    nativeAdSocialContext.setText(nativeBannerAd.getAdSocialContext());

    // You can use the following to specify the clickable areas.
    List<View> clickableViews = new ArrayList<>();
    clickableViews.add(nativeAdCallToAction);

    MediaView nativeAdIconView = adView.findViewById(R.id.native_icon_view);
    ImageView nativeImageViewAdIconView = adView.findViewById(R.id.image_view_icon_view);
    if (mUseImageView) {
      nativeAdIconView.setVisibility(View.GONE);
      nativeImageViewAdIconView.setVisibility(View.VISIBLE);
      nativeBannerAd.registerViewForInteraction(
          mNativeBannerAdContainer, nativeImageViewAdIconView, clickableViews);
    } else { // use MediaView
      nativeAdIconView.setVisibility(View.VISIBLE);
      nativeImageViewAdIconView.setVisibility(View.GONE);
      nativeBannerAd.registerViewForInteraction(
          mNativeBannerAdContainer, nativeAdIconView, clickableViews);
    }

    sponsoredLabel.setText(R.string.sponsored);
  }

  @Override
  public void onDestroy() {
    if (mNativeBannerAd != null) {
      mNativeBannerAd.unregisterView();
      mNativeBannerAd = null;
    }
    super.onDestroy();
  }
}
