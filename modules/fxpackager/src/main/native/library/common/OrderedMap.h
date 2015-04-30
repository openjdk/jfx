/*
 * Copyright (c) 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef ORDEREDMAP_H
#define ORDEREDMAP_H

#include <map>
#include <vector>
#include <assert.h>
#include <stdexcept>

#include <iostream>


template <typename _T1, typename _T2>
struct pair
{
    typedef _T1 first_type;
    typedef _T2 second_type;

    first_type first;
    second_type second;

    pair(first_type Value1, second_type Value2) {
        first = Value1;
        second = Value2;
    }
};


template <typename TKey, typename TValue>
class OrderedMap {
public:
    typedef TKey key_type;
    typedef TValue mapped_type;
    typedef pair<key_type, mapped_type> container_type;

private:
    typedef std::map<key_type, container_type*> map_type;
    typedef std::vector<container_type*> list_type;

public:
    typedef typename list_type::const_iterator const_iterator;

private:
    map_type FMap;
    list_type FList;

    typename list_type::iterator FindListItem(const key_type Key) {
        typename list_type::iterator result = FList.end();

        for (typename list_type::iterator iterator = FList.begin(); iterator != FList.end(); iterator++) {
            container_type *item = *iterator;

            if (item->first == Key) {
                result = iterator;
                break;
            }
        }

        return result;
    }

public:
    OrderedMap() {
    }

    OrderedMap(const OrderedMap<key_type, mapped_type> &Value) {
        Append(Value);
    }

    ~OrderedMap() {
        Clear();
    }

    void Clear() {
        for (typename list_type::iterator iterator = FList.begin(); iterator != FList.end(); iterator++) {
            container_type *item = *iterator;

            if (item != NULL) {
                delete item;
                item = NULL;
            }
        }

        FMap.clear();
        FList.clear();
    }

    bool ContainsKey(key_type Key) {
        bool result = false;

        if (FMap.find(Key) != FMap.end()) {
            result = true;
        }

        return result;
    }

    std::vector<key_type> GetKeys() {
        std::vector<key_type> result;

        for (typename list_type::const_iterator iterator = FList.begin();
             iterator != FList.end(); iterator++) {
            container_type *item = *iterator;
            result.push_back(item->first);
        }

        return result;
    }

    void Assign(const OrderedMap<key_type, mapped_type> &Value) {
        Clear();
        Append(Value);
    }

    void Append(const OrderedMap<key_type, mapped_type> &Value) {
        for (size_t index = 0; index < Value.FList.size(); index++) {
            container_type *item = Value.FList[index];
            Append(item->first, item->second);
        }
    }

    void Append(key_type Key, mapped_type Value) {
        container_type *item = new container_type(Key, Value);
        FMap.insert(std::pair<key_type, container_type*>(Key, item));
        FList.push_back(item);
    }

    bool RemoveByKey(key_type Key) {
        bool result = false;
        typename list_type::iterator iterator = FindListItem(Key);

        if (iterator != FList.end()) {
            FMap.erase(Key);
            FList.erase(iterator);
            result = true;
        }

        return result;
    }

    bool GetValue(key_type Key, mapped_type &Value) {
        bool result = false;
        container_type* item = FMap[Key];

        if (item != NULL) {
            Value = item->second;
            result = true;
        }

        return result;
    }

    bool SetValue(key_type Key, mapped_type &Value) {
        bool result = false;

        if (ContainsKey(Key) == true) {
            container_type *item = FMap[Key];

            if (item != NULL) {
                item->second = Value;
                result = true;
            }
        }
        else {
            Append(Key, Value);
            result = true;
        }

        return result;
    }

    mapped_type &operator[](key_type Key) {
        container_type* item = FMap[Key];
        assert(item != NULL);

        if (item != NULL) {
            return item->second;
        }

        throw std::invalid_argument("Key not found");
    }

    OrderedMap& operator= (OrderedMap &Value) {
        Append(Value);
        return *this;
    }
    
    OrderedMap& operator= (const OrderedMap &Value) {
        Append(Value);
        return *this;
    }

    size_t Count() {
        return FList.size();
    }

    typename OrderedMap::const_iterator begin() {
        return FList.begin();
    }

    typename OrderedMap::const_iterator end() {
        return FList.end();
    }
};

#endif //ORDEREDMAP_H
