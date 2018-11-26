# like-google-maps

Proof of concept; Google Maps like bottomsheet behavior

### What this project is
Inspired by [this](https://blog.picnic.nl/creating-well-behaved-views-in-android-3e088c560bc5) article on creating a custom bottomsheet behavior, this project aims to inspire and promote the use and creation of [CoordinatatorLayout Behaviors](https://developer.android.com/reference/android/support/design/widget/CoordinatorLayout.Behavior.html).

To achieve the final bottomsheet'esque behavior, the [orginal source](https://android.googlesource.com/platform/frameworks/support.git/+/master/design/src/android/support/design/widget/BottomSheetBehavior.java) for the bottomsheet behavior in the Android Design Library was copied, studied, and edited (arguably butchered). The orginal source didn't handle flings and / or transient states, as in, the bottomsheet was either hidden, collapsed or expanded. After any touch / drag interaction, the sheet would snap to a then determined state. 

Many different attempts led to unforeseen caveats and some unwanted side effects to overcome, particularly around nested scrolling and the allowance of a "sticky header". The inner workings of coordinator layout needed to be understood. A must watch [video](https://www.youtube.com/watch?v=x5o2hGMMmIw), takes a pretty deep dive into the inner workings of nested scrolling and CoordinatorLayout Behaviors. [This](https://www.androiddesignpatterns.com/2018/01/experimenting-with-nested-scrolling.html) article by Alex Lockwood on nested scrolling and some borrowed [code](https://github.com/alexjlockwood/adp-nested-scrolling) allowed me to finally achieve the end goal. 

The end result is a custom bottomsheet behavior that allows for an optional custom header. Snap can be enabled or disabled, as well as the peek height can be set to either _auto_, _header_ (height of the header) or a custom value(dp's). As with the original behavior, a flag exists for _hideable_. I also added a configurable _topOffset_ to prevent expanding past a certain y value.

One important takeaway from this experiment is that if you are creating your own behavior, it usually means the default behaviors in the Design Library are not accommodating to your requirements. So, likely, your end result will be _specific_ to your app's design and flow. We all strive to write robust and resuable code, and that principle should be applied to custom views and behaviors as well, but beware of the cost. The more customizations and options you allow and provide, the more complex your behavior becomes. You need to account for all permutations of state that can occur, and manage those.

peek=header; snap | peek=header; no snap
------------ | -------------
<img src="https://raw.githubusercontent.com/fish-4-fun/like-google-maps/master/external-assets/peek-header-snap.gif" width="320"> | <img src="https://raw.githubusercontent.com/fish-4-fun/like-google-maps/master/external-assets/peek-header-no-snap.gif" width="320">

peek=auto; snap | peek=auto; snap; hideable 
------------ | -------------
<img src="https://raw.githubusercontent.com/fish-4-fun/like-google-maps/master/external-assets/peek-auto-snap.gif" width="320"> | <img src="https://raw.githubusercontent.com/fish-4-fun/like-google-maps/master/external-assets/peek-auto-snap-hideable.gif" width="320">
