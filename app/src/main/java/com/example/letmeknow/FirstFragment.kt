package com.example.letmeknow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.letmeknow.databinding.FragmentFirstBinding
import com.example.letmeknow.messages.BaseMessage

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        fun successCallback(msg: BaseMessage) {

        }

        fun failureCallback(msg: BaseMessage) {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            /* val msg=UserMessage(30)
             msg.from= UUID(0,1);
             msg.to=UUID(0,0)
             messageHandler.offerMessage(msg)
             while(!messageHandler.Sucess){
                 Toast.makeText(view.context.applicationContext, "Data saved", Toast.LENGTH_LONG).show();

             }*/
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}