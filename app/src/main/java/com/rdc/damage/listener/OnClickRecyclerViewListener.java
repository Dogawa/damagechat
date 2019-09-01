package com.rdc.damage.listener;


public interface OnClickRecyclerViewListener {

    /**
     * 点击item
     * @param position
     */
    void onItemClick(int position);

    /**
     * 长按item
     * @param position
     * @return
     */
    boolean onItemLongClick(int position);

}
