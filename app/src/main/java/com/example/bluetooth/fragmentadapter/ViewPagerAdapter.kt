package com.example.bluetooth.fragmentadapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter(supportFragmentManager: FragmentManager) : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT )  {

    val mFragmentList=ArrayList<Fragment>()
    val mFragmentsTitleList= ArrayList<String>()

    override fun getItem(position: Int): Fragment {
        return mFragmentList[position]
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mFragmentsTitleList[position]
    }

    fun addFragments(fragment: Fragment, title: String){
        mFragmentList.add(fragment)
        mFragmentsTitleList.add(title)
    }
}