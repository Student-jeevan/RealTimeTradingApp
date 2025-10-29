import React from 'react';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { DragHandleHorizontalIcon} from "@radix-ui/react-icons"
const Navbar = ()=>{
    return (
        <div className='px-2 py-3 border-b z-50 bg-background bg-opacity-0 sticky top-0 left-0 right-0 flex justify-between items-center'>

            <div className ='flex items-center gap-3'>
                <Sheet>
                    <SheetTrigger>
                        <Button variant="ghost" size="icon" className="rounded-full h-11 w-11">
                            <DragHandleHorizontalIcon className ='h-7 w-7' />
                        </Button>
                    </SheetTrigger>
                    <SheetContent side="left">
                        <SheetHeader>
                        <SheetTitle>Are you absolutely sure?</SheetTitle>
                        <SheetDescription>
                            This action cannot be undone. This will permanently delete your account
                            and remove your data from our servers.
                        </SheetDescription>
                        </SheetHeader>
                    </SheetContent>
                </Sheet>
            </div>

        </div>
    )
}
export default Navbar;