package com.example.letmeknow.util

class BiMap<A,B>{
    private val  a_b= HashMap<A, B>()
    private val  b_a= HashMap<B, A>()
    fun put(a:A,b:B){
        a_b.put(a,b)
        b_a.put(b,a)
    }
    fun getAB(a:A):B?{
        return a_b[a]
    }
    fun getBA(b:B):A?{
        return b_a[b]
    }
    fun containsEither(a:A,b:B):Boolean{
        return a_b.containsKey(a) || b_a.containsKey(b)
    }
    fun iterA():Iterable<A>{
        return a_b.keys
    }
    fun iterB():Iterable<B>{
        return b_a.keys;
    }
}